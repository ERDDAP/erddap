/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.Set;

/**
 * This class holds a list of attributes (name=value, where name is a String
 * and value is a PrimitiveArray). 
 * The backing datastructure (ConcurrentHashMap) is thread-safe.
 *
 * @author Bob Simons (CoHortSoftware@gmail.com)
 */
public class Attributes {
    //FUTURE: implement as ArrayString for names and ArrayList for values?

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean debugMode = false;

    /** The backing data structure.  It is thread-safe. */
    private ConcurrentHashMap<String,PrimitiveArray> hashmap = new ConcurrentHashMap(16, 0.75f, 4);

    /**
     * This constructs a new, empty Attributes object.
     */
    public Attributes() {
    }
    
    /**
     * This constructs a new Attributes object which is a clone of 
     *   'attributes'.
     *
     * @param tAtts a set of attributes, each of which will be cloned 
     *   to make a new Attributes object.
     */
    public Attributes(Attributes tAtts) {
        if (tAtts == null) 
            String2.log("!WARNING: tAtts is null in Attributes(tAtts).");  //do nothing
        else tAtts.copyTo(this);
    }
    
   
    /**
     * This constructs a new Attributes object which has the
     * contents of moreImportant and lessImportant.
     *
     * @param moreImportant the basis for a new Attributes. If one of the keys
     *   is the same as in lessImportant, the value from this takes precedence.
     * @param lessImportant the basis for a new Attributes.
     */
    public Attributes(Attributes moreImportant, Attributes lessImportant) {
        lessImportant.copyTo(this);
        set(moreImportant); //duplicate keys in 'more' will overwrite keys from 'less'
    }
    


    /**
     * This clears all the attributes being held.
     * The result isValid().
     */
    public void clear() {
        hashmap.clear();  
    }

    /**
     * This returns the number of nameValue pairs in the data structure.
     *
     * @return the number of nameValue pairs in the data structure.
     */
    public int size() {
        return hashmap.size();
    }

    /**
     * This makes a deep clone of the current table (data and attributes).
     *
     * @return a new Table.
     */
    public Object clone() {
        Attributes attributes2 = new Attributes();
        this.copyTo(attributes2);
        return attributes2;
    }

    /**
     * This returns the value of a specific attribute (or null
     * if the name isn't defined).
     * 
     * @param name the name (key) of the desired attribute
     * @return the attribute's value (a PrimitiveArray).
     */
    public PrimitiveArray get(String name) {
        return hashmap.get(name);
    }

    /**
     * This returns an array with the names of all of the attributes.
     * 
     * @return an array with the names of all of the attributes, sorted in 
     *     a case-insensitive way.
     */
    public String[] getNames() {
        StringArray names = new StringArray(hashmap.keys());
        names.sortIgnoreCase();
        return names.toArray();
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a String, regardless of the type used to store it.
     *
     * @param name the name of an attribute
     * @return the String attribute or null if trouble (e.g., not found).
     *   This uses pa.getRawString(0) so e.g., 127 in ByteArray will appear as "127".
     */
    public String getString(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return null;
            return pa.getRawString(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A convenience method which assumes the first element of the attribute's 
     * value PrimitiveArray is a CSV String and which splits the string into parts.
     *
     * @param name the name of an attribute
     * @return a String[] or null if trouble (e.g., not found)
     */
    public String[] getStringsFromCSV(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return null;
            String csv = pa.getRawString(0);
            return StringArray.arrayFromCSV(csv);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a double, regardless of the type used to store it.
     *
     * @param name the name of an attribute
     * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
     */
    public double getDouble(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Double.NaN;
            return pa.getDouble(0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a double, regardless of the type used to store it.
     * In this "unsigned" variant, if pa isIntegerType, then the value
     * is interpreted as an unsigned value (two's compliment, e.g., -1B becomes 255).
     * Note that the native cohort missing value (e.g., 127B) is treated as a value,
     * not a missing value.
     *
     * @param name the name of an attribute
     * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
     */
    public double getUnsignedDouble(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Double.NaN;
            return pa.getUnsignedDouble(0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a double, regardless of the type used to store it.
     * In this "raw" variant, if pa isIntegerType, then the cohort missingValue 
     * (e.g., ByteArray missingValue=127) is left intact 
     * and NOT converted to new pa's missingValue (e.g., Double.NaN).
     *
     * @param name the name of an attribute
     * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
     */
    public double getRawDouble(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Double.NaN;
            return pa.getRawDouble(0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a double, regardless of the type used to store it.
     * If the type was float, this returns Math2.floatToDouble(value).
     *
     * @param name the name of an attribute
     * @return the attribute as a nice double or Double.NaN if trouble (e.g., not found).
     *    E.g., ByteArray 127 will be returned as Double.NaN.
     */
    public double getNiceDouble(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Double.NaN;
            return pa.getNiceDouble(0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a float, regardless of the type used to store it.
     *
     * @param name the name of an attribute
     * @return the attribute as a float or Float.NaN if trouble (e.g., not found)
     */
    public float getFloat(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Float.NaN;
            return pa.getFloat(0);
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a long, regardless of the type used to store it.
     *
     * @param name the name of an attribute
     * @return the attribute as a long or Long.MAX_VALUE if trouble (e.g., not found)
     */
    public long getLong(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Long.MAX_VALUE;
            return pa.getLong(0);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as an int, regardless of the type used to store it.
     *
     * @param name the name of an attribute
     * @return the attribute as an int or Integer.MAX_VALUE if trouble (e.g., not found)
     */
    public int getInt(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Integer.MAX_VALUE;
            return pa.getInt(0);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as an int, regardless of the type used to store it.
     * In this "raw" variant, if pa isIntegerType, then the cohort missingValue 
     * (e.g., ByteArray missingValue=127) is left intact 
     * and NOT converted to new pa's missingValue (e.g., Double.NaN).
     *
     * @param name the name of an attribute
     * @return the attribute as an int or Integer.MAX_VALUE if trouble (e.g., not found)
     */
    public int getRawInt(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Integer.MAX_VALUE;
            return pa.getRawInt(0);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * This removes a specific attribute.
     * 
     * @param name the name of the attribute
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray remove(String name) {
        return hashmap.remove(name);
    }

    /**
     * This is the main method to set the value of a specific attribute (adding it if it
     * doesn't exist, revising it if it does, or removing it if value is
     * (PrimitiveArray)null).
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or size==0 or it is one String="", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, PrimitiveArray value) {
        if (value == null || value.size() == 0 || 
            (value.size() == 1 && value instanceof StringArray && value.getRawString(0).trim().length() == 0)) 
            return hashmap.remove(name);
        return hashmap.put(String2.canonical(name), value);
    }

    /** 
     * Like set, but only sets the value if there is no current value.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or "", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray setIfNotAlreadySet(String name, PrimitiveArray value) {
        PrimitiveArray pa = get(name);
        if (pa != null)
            return pa;
        return set(name, value);
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, PrimitiveArray value) {
        set(name, value);
        return this;
    }

    /**
     * This calls set() for all the attributes in 'additional'.
     * In case of duplicate names, the 'additional' attributes have precedence.
     *
     * @param moreImportant the Attributes to be added.
     */
    public void set(Attributes moreImportant) {
        Enumeration en = moreImportant.hashmap.keys();
        while (en.hasMoreElements()) {
            String name = (String)en.nextElement();
            set(name, (PrimitiveArray)moreImportant.get(name).clone());
        }
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param moreImportant the Attributes to be added
     * @return 'this'
     */
    public Attributes add(Attributes moreImportant) {
        set(moreImportant);
        return this;
    }

    /** 
     * A convenience method which stores the object then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a Byte, Short, ... String.
     *    If value is null or "", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, Object value) {
        if (value == null) 
            return remove(name);
        return set(name, PrimitiveArray.factory(value));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a String which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, Object value) {
        set(name, value);
        return this;
    }

    /** 
     * A convenience method which stores the String in a StringArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or "", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, String value) {
        //netCDF doesn't allow 0 length strings
        if (value == null || value.trim().length() == 0) 
            return remove(name);
        return set(name, new StringArray(new String[]{value})); //new StringArray calls String2.canonical
    }

    /** 
     * Like set, but only sets the value if there is no current value.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or "", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray setIfNotAlreadySet(String name, String value) {
        PrimitiveArray pa = get(name);
        if (pa != null)
            return pa;
        return set(name, value);
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a String which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, String value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the double in a DoubleArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, double value) {
        return set(name, new DoubleArray(new double[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a double which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, double value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the float in a FloatArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, float value) {
        return set(name, new FloatArray(new float[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a float which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, float value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the long in an LongArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, long value) {
        return set(name, new LongArray(new long[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a long which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, long value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the int in an IntArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, int value) {
        return set(name, new IntArray(new int[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value an int which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, int value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the short in an ShortArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, short value) {
        return set(name, new ShortArray(new short[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a short which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, short value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the char in an CharArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, char value) {
        return set(name, new CharArray(new char[]{value}));
    }
    
    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a char which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, char value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the byte in an ByteArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, byte value) {
        return set(name, new ByteArray(new byte[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a byte which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, byte value) {
        set(name, value);
        return this;
    }

    /** This prints the attributes to a newline separated String, one per line: "&nbsp;&nbsp;&nbsp;&nbsp;[name]=[value]".*/
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String names[] = getNames();
        for (int i = 0; i < names.length; i++) {
            sb.append("    " + names[i] + "=" + get(names[i]).toNccsvAttString() + "\n");
        }
        return sb.toString();
    }

    /** 
     * This removes any entry which has a String value of 'value'. 
     * value must equal the pa.toString() of the attribute's value,
     * so a string with the word null must be requesth here as "\"null\""
     *
     * @param value Any attribute that has this value (when evaluated as a String)
     *   will be removed.
     */
    public void removeValue(String value) {
        Iterator it = hashmap.keySet().iterator(); //iterator (not enumeration) since I use it.remove() below
        while (it.hasNext()) {
            String name = (String)it.next();
            //String2.log(">> lookFor=" + value + " found=" + get(name).toString());
            if (get(name).toString().equals(value))
                it.remove();
        }
    }

    /**
     * This generates a String with 
     * "[prefix][name]=[value][suffix]" on each line.
     *
     * <p>This nc-style version is used to print netcdf header attributes. 
     * It uses String2.toJson for String attributes. 
     *
     * @param prefix The text to be precede "[name]=[value]" on each line,
     *   perhaps "".
     * @param suffix The text to follow "[name]=[value]" on each line,
     *   perhaps "".
     * @return the desired string representation
     */
    public String toNcString(String prefix, String suffix) {
//<p>Was: It puts quotes around String attributeValues,
// converts \ to \\, and " to \", and appends 'f' to float values.
        StringBuilder sb = new StringBuilder();
        String names[] = getNames();
        for (int index = 0; index < names.length; index++) {
            sb.append(prefix + names[index] + " = ");
            PrimitiveArray pa = hashmap.get(names[index]);
            String connect = "";
            boolean isCharArray = pa instanceof CharArray;
            if (pa instanceof StringArray || isCharArray) {
                int n = pa.size();
                for (int i = 0; i < n; i++) {
                    sb.append(connect);
                    connect = ", ";
                    sb.append(String2.toJson(pa.getRawString(i), 65536, isCharArray)); //encodeNewline?
                }
            } else if (pa instanceof FloatArray) {
                FloatArray fa = (FloatArray)pa;
                int n = fa.size();
                for (int i = 0; i < n; i++) {
                    sb.append(connect);
                    connect = ", ";
                    sb.append(fa.get(i));
                    sb.append("f");
                }
            } else {
                sb.append(pa.toString());
            }
            sb.append(suffix + "\n");
        }
        return sb.toString();
    }

    /**
     * This makes destination's contents equal this Attribute's contents.
     * The values (primitiveArrays) are cloned.
     *
     * @param destination the Attributes which will be made equal to 'source'.
     */
    public void copyTo(Attributes destination) {
        destination.hashmap.clear();
        Enumeration en = hashmap.keys();
        while (en.hasMoreElements()) {
            String name = (String)en.nextElement();
            destination.set(name, (PrimitiveArray)get(name).clone());
        }
    }

    /** 
     * This tests if o is an Attributes and has the same data. 
     * This doesn't throw an Exception if a difference is found.
     *
     * @param o an object, presumably an Attributes
     */
    public boolean equals(Object o) {
        return testEquals(o).length() == 0;
    }

    /** 
     * This returns a string indicating the differents of this Attributes and o,
     * or "" if no difference. 
     * This doesn't throw an Exception if a difference is found.
     *
     * @param o an object, presumably an Attributes
     * @return a string indicating the differents of this Attributes and o,
     *   or "" if no difference. 
     */
    public String testEquals(Object o) {
        if (o == null)
            return "The new Attributes object is null.";
        if (!(o instanceof Attributes))
            return "The new object isn't an Attributes object.";
        return Test.testEqual(toString(), ((Attributes)o).toString(), "");
    }

    /**
     * This returns a netcdf-style String representation of a PrimitiveArray:
     * StringArray is newline separated, others are comma separated.
     *
     * @param pa any PrimitiveArray
     * @return a String representation of a value PrimitiveArray.
     */
    public static String valueToNcString(PrimitiveArray pa) {
        if (pa instanceof StringArray)
            return String2.toSVString(((StringArray)pa).toStringArray(), "\n", false); //false=no trailing newline
        return pa.toString();
    }

    /**
     * This goes through all of this Atts (addAtts?) and otherAtts (sourceAtts?) and 
     * adds a trim'd version to this Atts if any att has whitespace
     * at beginning or end.
     * 
     * @param otherAtts another Attributes object
     */
    public void trimIfNeeded(Attributes otherAtts) {
        if (otherAtts == null)
            return;

        //go through all atts in this or otherAtts
        HashSet set = new HashSet();
        set.addAll(otherAtts.hashmap.keySet());
        set.addAll(hashmap.keySet());
        Iterator it = set.iterator();
        while (it.hasNext()) {
            String name = (String)it.next();
            PrimitiveArray pa = get(name); //prefer from this Atts
            if (pa == null) 
                pa = otherAtts.get(name);
            if (pa == null)
                continue;
            if (pa.elementClass() == String.class) {
                boolean changed = false;
                int tSize = pa.size();                    
                StringArray newSa = new StringArray();
                for (int i = 0; i < tSize; i++) {
                    String s = pa.getString(i);
                    String ts = null;
                    if (s != null) {
                        ts = i == 0? s.trim() : String2.trimEnd(s);
                        if (ts.length() < s.length())
                            changed = true;
                    }
                    newSa.add(ts);
                }
                if (changed) 
                    set(name, newSa);
            }
        }
    }

    /**
     * This removes keys and values from this Attributes which 
     * are exactly equal (even same data type) in otherAtts.
     * 
     * @param otherAtts another Attributes object
     */
    public void removeIfSame(Attributes otherAtts) {
        if (otherAtts == null)
            return;

        //go through this 
        Iterator it = hashmap.keySet().iterator(); //iterator (not enumeration) since I use it.remove() below
        while (it.hasNext()) {
            String name = (String)it.next();
            PrimitiveArray otherPa = otherAtts.get(name);
            if (otherPa != null) {
                PrimitiveArray pa = get(name);
                if (pa.equals(otherPa)) 
                    //if atts are equal, remove in this Attributes
                    it.remove();
            }
        }
    }

    /**
     * This trim()s all names/keys and trim()s all 1 String values.
     */
    public void trim() {
        Iterator it = hashmap.keySet().iterator(); 
        while (it.hasNext()) {
            String name = (String)it.next();
            String tName = name.trim();
            PrimitiveArray pa = null;
            if (name.equals(tName)) {
                pa = get(name);
            } else {
                //switch to trim'd name
                pa = remove(name);
                set(name = tName, pa);
            }

            //trim value?
            if (pa instanceof StringArray && pa.size() > 0) {
                pa.setString(0, String2.trimStart(pa.getRawString(0)));
                pa.setString(pa.size() - 1, String2.trimEnd(pa.getRawString(pa.size() - 1)));
            }
        }

    }

    /**
     * This trim()s and makesValidUnicode all names/keys and all 1 String values.
     * For multi-string values, this makes them valid Unicode.
     */
    public void trimAndMakeValidUnicode() {
        Iterator it = hashmap.keySet().iterator(); 
        while (it.hasNext()) {
            String name = (String)it.next();
            String tName = String2.makeValidUnicode(name.trim(), "\r\n\t");

            PrimitiveArray pa = null;
            if (name.equals(tName)) {
                pa = get(name);
            } else {
                //switch to trim/valid name
                pa = remove(name);
                set(name = tName, pa);
            }

            //trim/makeValid the value?
            if (pa instanceof StringArray && pa.size() > 0) {
                int tSize = pa.size();
                pa.setString(0, String2.trimStart(pa.getRawString(0)));
                pa.setString(tSize - 1, String2.trimEnd(pa.getRawString(tSize - 1)));
                for (int i = 0; i < tSize; i++) {
                    String s = String2.makeValidUnicode(pa.getRawString(i), "\r\n\t");
                    s = String2.commonUnicodeToPlainText(s); //a little aggressive, but reasonable for attributes
                    pa.setString(0, s);
                }
            }
        }

    }

    /**
     * This uses StringArray.fromNccsv() on all StringArray values
     * to de-JSON and convert "" to ".
     */
    public void fromNccsvStrings() {
        Iterator it = hashmap.keySet().iterator(); 
        while (it.hasNext()) {
            String name = (String)it.next();
            PrimitiveArray pa = get(name);
            if (pa.elementClass() == String.class) 
                ((StringArray)pa).fromNccsv();
        }
    }

    /**
     * This makes a set of addAttributes which are needed to change a into b.
     * If an attribute in 'a' needs to be set to null, this sets it to the String 
     * "null" instead of just nulling it.
     *
     * @param a an Attributes object
     * @param b another Attributes object
     * @return a set of Attributes which are needed to change a into b.
     */
    public static Attributes makeALikeB(Attributes a, Attributes b) {
        Attributes addAtts = new Attributes();

        //remove/change atts already in 'a' that aren't correct
        String[] aNames = a.getNames();
        int naNames = aNames.length;
        for (int i = 0; i < naNames; i++) {
            String aName = aNames[i];
            PrimitiveArray aPA = a.get(aName);
            PrimitiveArray bPA = b.get(aName);
            if (bPA == null)
                addAtts.set(aName, "null"); 
            else if (!aPA.equals(bPA))
                addAtts.set(aName, bPA);
        }

        //add atts from 'b' that aren't already in 'a'
        String[] bNames = b.getNames();
        int nbNames = bNames.length;
        for (int i = 0; i < nbNames; i++) {
            String bName = bNames[i];
            if (a.get(bName) == null)
                addAtts.set(bName, b.get(bName));
        }

        return addAtts;
    }

    /**
     * This writes the attributes for a variable (or *GLOBAL*) to an NCCSV String.
     * This doesn't write *SCALAR* or dataType attributes.
     * This doesn't change any of the attributes.
     * 
     * @param varName The name of the variable to which these attributes are associated.
     * @return a string with all of the attributes for a variable (or *GLOBAL*) 
     *   formatted for NCCSV.
     */
    public String toNccsvString(String varName) {
        String nccsvVarName = String2.toNccsvDataString(varName);
        StringBuilder sb = new StringBuilder();
        String tName;

        //special case: *GLOBAL* Conventions
        if (varName.equals(String2.NCCSV_GLOBAL)) {
            tName = "Conventions";
            String val = getString(tName);
            if (String2.isSomething(val)) {
                if (val.indexOf("NCCSV") < 0)
                    val += ", " + String2.NCCSV_VERSION;
            } else {
                val = "COARDS, CF-1.6, ACDD-1.3, " + String2.NCCSV_VERSION;
            }
            sb.append(
                String2.toNccsvDataString(varName) + "," +  
                String2.toNccsvDataString(tName)   + "," +  
                String2.toNccsvAttString(val)     + "\n"); 
        }

        //each of the attributes
        String names[] = getNames();
        for (int ni = 0; ni < names.length; ni++) { 
            tName = names[ni];
            if (varName.equals(String2.NCCSV_GLOBAL) && tName.equals("Conventions"))
                continue;
            if (!String2.isSomething(tName) || 
                tName.equals("_NCProperties")) 
                continue;
            PrimitiveArray tValue = get(tName);
            if (tValue == null || tValue.size() == 0 || tValue.toString().length() == 0) 
                continue; //do nothing
            sb.append(
                String2.toNccsvDataString(nccsvVarName) + "," + 
                String2.toNccsvDataString(tName)        + "," +  
                tValue.toNccsvAttString()           + "\n"); 
        }
        return sb.toString();        
    }

    /**
     * This writes the attributes for a variable (or *GLOBAL*) to an NCCSV Binary
     * DataOutputStream.
     * This doesn't write *SCALAR* or dataType attributes.
     * This doesn't change any of the attributes.
     * For each attribute, this writes: varName, attName, attPA.
     * 
     * @param dos the DataOutputStream to be written to.
     * @param varName The name of the variable to which these attributes are associated.
     */
/* project not finished or tested
    public void writeNccsvDos(DataOutputStream dos, String varName) throws Exception {
        String tName;

        //special case: *GLOBAL* Conventions
        if (varName.equals(String2.NCCSV_GLOBAL)) {
            String2.writeNccsvDos(dos, varName);
            tName = "Conventions";
            String2.writeNccsvDos(dos, tName);
            String val = getString(tName);
            if (String2.isSomething(val)) {
                if (val.indexOf("NCCSV") < 0)
                    val += ", " + String2.NCCSV_BINARY_VERSION;
            } else {
                val = "COARDS, CF-1.6, ACDD-1.3, " + String2.NCCSV_BINARY_VERSION;
            }
            StringArray sa = new StringArray();
            sa.add(val);
            sa.writeNccsvDos(dos); 
        }

        //each of the attributes
        String names[] = getNames();
        for (int ni = 0; ni < names.length; ni++) { 
            tName = names[ni];
            if (varName.equals(String2.NCCSV_GLOBAL) && tName.equals("Conventions"))
                continue;
            if (!String2.isSomething(tName) || 
                tName.equals("_NCProperties")) 
                continue;
            PrimitiveArray tValue = get(tName);
            if (tValue == null || tValue.size() == 0 || tValue.toString().length() == 0) 
                continue; //do nothing
            String2.writeNccsvDos(dos, varName);
            String2.writeNccsvDos(dos, tName);
            tValue.writeNccsvDos(dos); 
        }
    }
*/
    /**
     * This writes the attributes for a variable (or *GLOBAL*) to a String using
     * NCO JSON lvl=2 pedantic style.
     * See http://nco.sourceforge.net/nco.html#json
     * This doesn't change any of the attributes.
     * See issues in javadoc for EDDTable.saveAsNcoJson().
     *
     * <p>String attributes are written as type="char". 
     * See comments in EDDTable.
     * 
     * @param indent a String a spaces for the start of each line
     * @return a string with all of the attributes for a variable (or *GLOBAL*) 
     *   formatted for NCO JSON, with a comma after the closing }.
     */
    public String toNcoJsonString(String indent) {
        StringBuilder sb = new StringBuilder();

//"attributes": {
//  "byte_att": { "type": "byte", "data": [0, 1, 2, 127, -128, -127, -2, -1]},
//  "char_att": { "type": "char", "data": "Sentence one.\nSentence two.\n"},
//  "short_att": { "type": "short", "data": 37},
//  "int_att": { "type": "int", "data": 73},
//  "long_att": { "type": "int", "data": 73},                            //???!!! why not "type"="long"???       
//  "float_att": { "type": "float", "data": [73.0, 72.0, 71.0, 70.010, 69.0010, 68.010, 67.010]},
//  "double_att": { "type": "double", "data": [73.0, 72.0, 71.0, 70.010, 69.0010, 68.010, 67.0100010]}
//},
        sb.append(indent + "\"attributes\": {\n");

        //each of the attributes
        String names[] = getNames();
        boolean somethingWritten = false;
        for (int ni = 0; ni < names.length; ni++) { 
            String tName = names[ni];
            PrimitiveArray pa = get(tName);
            if (pa == null || pa.size() == 0) 
                continue; //do nothing
            String tType = pa.elementClassString();
            boolean isString = tType.equals("String");
            //In NCO JSON, all char and String attributes appear as 1 string with type=char.
            //There are no "string" attributes.
            if (isString) {
                tType = "char";
            } else if (tType.equals("long")) {
                tType = "int64"; //see https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_data_types and NCO JSON examples
            }
            sb.append(
                (somethingWritten? ",\n" : "") +
                indent + "  " +
                String2.toJson(tName) + ": {\"type\": \"" + tType + "\", \"data\": ");
            if (isString) {
                String s = ((StringArray)pa).toNewlineString();
                sb.append(String2.toJson(s.substring(0, s.length() - 1))); //remove trailing \n
            } else if (pa.elementClass() == char.class) {
                //each char becomes a string, then displayed as newline separated string
                String s = (new StringArray(pa)).toNewlineString();
                sb.append(String2.toJson(s.substring(0, s.length() - 1))); //remove trailing \n
            } else {
                String js = pa.toJsonCsvString();
                sb.append(pa.size() == 1?  js :
                    "[" + js + "]");
            }
            sb.append('}');
            somethingWritten = true;
        }
        sb.append(
            (somethingWritten? "\n" : "") +  //end previous line
            indent + "},\n");
        return sb.toString();        
    }

    /**
     * This is standardizes one data variable's atts and pa.
     *
     * @param standardizeWhat This determines what gets done.
     *   It is the sum of these options (when applicable):
     *   <br>1: apply scale_factor, add_offset, 
     *          make _Unsigned into signed, e.g., unsigned bytes become signed shorts,
     *          convert defined numeric _FillValue's and missing_value's to "standard" values
     *          (MAX_VALUE for integer types, and NAN for doubles and floats),
     *          and unpack packed attributes (e.g., actual|data|valid_min|max|range) (if packed).
     *   <br>2: convert defined numeric dateTimes to "seconds since 1970-01-01T00:00:00Z".
     *      (If this is selected and there is a chance the data has scale_factor or add_offset, 
     *      #1 must be selected also.)
     *   <br>4: convert defined string _FillValue's and missing_value's to "".
     *  
     *   <br>256: if dataPa is numeric,  if _FillValue and missing_value aren't defined,
     *      try to identify an undefined numeric missing_value (-999?, 9999?, 1e37f?) 
     *      and convert instances to "standard" values: MAX_VALUE for integer types, and NAN for doubles and floats.
     *      This is done before #1 and #2.
     *   <br>512: if dataPa is Strings, convert all common, undefined, missing value strings 
     *      (e.g., ".", "...", "-", "?", "???", "N/A", "none", "null", "unknown", "unspecified" 
     *      (it is case-insensitive)) to "".   "nd" and "other" are not on the list.
     *      See code for String2.isSomething2() for the current list.
     *   <br>1024: if dataPa is Strings, try to convert not-purely-numeric String dateTimes 
     *       (e.g., "Jan 2, 2018") to ISO 8601 String dateTimes.
     *   <br>2048: if dataPa is Strings, try to convert purely-numeric String dateTimes
     *       (e.g., "20180102") to ISO 8601 String dateTimes.
     *   <br>4096: This tries to standardize the units string for each variable.
     *       For example, "meters per second", "meter/second", "m.s^-1", "m s-1", "m.s-1"
     *       will all be converted to "m.s-1". This doesn't change the data values.
     *       This works well for valid UDUNITS
     *       units strings, but can have problems with invalid or complex strings.
     *       You can deal with problems by specifying specific from-to pairs
     *       in &lt;standardizeUdunits&gt; in messages.xml.  Please email any changes
     *       you make to bob.simons at noaa.gov so they can be incorporated
     *       into the default messages.xml.
     *   <br>So a common, do-all-safe-things value is: 7 (1+2+4).
     *   <br>And if standardizeWhat == 0, this just returns dataPa.
     * @param varName is for diagnostic messages only
     * @param dataPa the (possibly packed) pa for the variable
     * @return the same dataPa or a new dataPa of a different type.
     * @throws Exception if trouble
     */
    public PrimitiveArray standardizeVariable(int standardizeWhat, String varName, PrimitiveArray dataPa) throws Exception {
        //I had alternatives to 1024 and 2048 that converted String times
        //to epochSeconds, but removed them because:
        //many users e.g., EDDTableFromAsciiFiles has already converted 
        //the columns to the destination data type (e.g., double) before
        //doing the standardize (which would expect strings). 
        //Rather than deal will complications about which options can be used
        //when, I just kept the options (1024 and 2048) that keep the dataType the same.

        if (standardizeWhat == 0)
            return dataPa;
        if ((standardizeWhat & 2) != 0 &&
            (standardizeWhat & 1) == 0)
            throw new RuntimeException(String2.ERROR + 
                " in Attributes.standardizeVariable: if standardizeWhat(" + standardizeWhat + ") includes 2, it must also include 1.");
        if (debugMode) String2.log(">> standardizeVariable varName=" + varName);
        Attributes newAtts = this;   //so it has a more descriptive name     
        Attributes oldAtts = new Attributes(this); //so we have an unchanged copy to refer to
        String oUnits = oldAtts.getString("units");

        //determine what the destClass will be (for packed numeric variables)
        Class oClass = dataPa.elementClass();
        boolean isNumeric = oClass != char.class && oClass!= String.class;
        boolean isString = oClass == String.class;
        Class destClass = oClass; 

        //determine if this is numeric dateTimes that will be converted to epochSeconds
        boolean standardizeNumericDateTimes = (standardizeWhat & 2) == 2 && isNumeric && 
            Calendar2.isNumericTimeUnits(oUnits);

        //if standardizeWhat & 256, if _FillValue and missing_value aren't defined,
        //    try to identify an undefined numeric missing_value (-999?, 9999?, 1e37f?) 
        //    and convert them to "standard" values: MAX_VALUE for integer types, and NAN for floats.
        //Do this BEFORE #1 and #2
        if ((standardizeWhat & 256) == 256 && isNumeric) {
            PrimitiveArray mvPA = oldAtts.get("missing_value");
            PrimitiveArray fvPA = oldAtts.get("_FillValue");
            if (mvPA == null && fvPA == null) {
                //look for missing_value = -99, -999, -9999, -99999, -999999, -9999999 
                double mv = dataPa.tryToFindNumericMissingValue();
                if (!Double.isNaN(mv)) {
                    dataPa.switchFromTo("" + mv, "");
                    if (debugMode) String2.log(
                        ">> #256: switched mv=" + mv + " to \"standard\" mv.");
                }
            }
        }

        //if standardizeWhat & 1, apply _Unsigned, scale_factor, and add_offset; change missing_value, _FillValue
        if ((standardizeWhat & 1) == 1 && isNumeric) {
            PrimitiveArray unsignedPA = newAtts.remove("_Unsigned");
            PrimitiveArray scalePA    = newAtts.remove("scale_factor");
            PrimitiveArray addPA      = newAtts.remove("add_offset");
            boolean unsigned = unsignedPA != null && "true".equals(unsignedPA.toString());         
            double scale = 1;
            double add = 0;
            if (scalePA != null) {  
                scale = scalePA.getNiceDouble(0);
                if (Double.isNaN(scale))
                    scale = 1;
            }
            if (addPA != null) {
                add = addPA.getNiceDouble(0);
                if (Double.isNaN(add))
                    add = 0;
            }

            //get _FillValue and missing_value 
            //If dataPa is unsigned, these are already unsigned
            //If dataPa is packed, these are packed too.
            double dFillValue    = unsigned?
                oldAtts.getUnsignedDouble("_FillValue") :    //so -1B becomes 255
                oldAtts.getDouble("_FillValue");    
            double dMissingValue = unsigned?
                oldAtts.getUnsignedDouble("missing_value") : //so -1B becomes 255
                oldAtts.getDouble("missing_value");                              
            newAtts.remove("missing_value");
            newAtts.remove("_FillValue");

            if (unsigned || scalePA != null || addPA != null) {
                if (debugMode && unsigned) String2.log(
                    ">> #1: unsigned=true");

                //make changes
                destClass = 
                    standardizeNumericDateTimes? double.class :
                    scalePA != null? scalePA.elementClass() :
                    addPA   != null? addPA.elementClass() : 
                    unsigned && oClass == byte.class?  short.class :  //similar code below
                    unsigned && oClass == short.class? int.class :
                    unsigned && oClass == int.class?   double.class : //ints are converted to double because nc3 doesn't support long
                    unsigned && oClass == long.class?  double.class : //longs are converted to double (not ideal)
                    destClass;  

                //switch data type
                dataPa = 
                    //if stated missing_value or _FillValue is same as cohort missingValue...
                    unsigned? 
                        PrimitiveArray.unsignedFactory(destClass, dataPa) : //unsigned 
                        dataPa.isIntegerType() &&
                            (dMissingValue == dataPa.missingValue() ||  //e.g., 127 == 127
                             dFillValue    == dataPa.missingValue())?
                            PrimitiveArray.factory(        destClass, dataPa) : //missingValues (e.g., 127) are    changed, e.g., to NaN
                            PrimitiveArray.rawFactory(     destClass, dataPa);  //missingValues (e.g., 127) AREN'T changed
                //so unsigned values are now properly unsigned

                //If present, missing_value and _FillValue are packed
                //so convert other dMissingValue and dFillValue (e.g., -128) to PA standard mv
                //after changing class (so unsigned is applied) but before applying scaleAddOffset
                //String2.log("> dFillValue=" + dFillValue + " dataPa.missingValue=" + dataPa.missingValue());
                if (!Double.isNaN(dMissingValue) && dMissingValue != dataPa.missingValue()) 
                    dataPa.switchFromTo(       "" + dMissingValue, "");
                if (!Double.isNaN(dFillValue)    && dFillValue    != dataPa.missingValue()) 
                    dataPa.switchFromTo(       "" + dFillValue,    "");

                //apply scaleAddOffset
                if (scale != 1 || add != 0) {
                    dataPa.scaleAddOffset(scale, add); //it checks for (1,0).  CoHort missing values are unaffected.
                    if (debugMode) String2.log(
                        ">> #1: scale_factor=" + scale + " add_offset=" + add);
                }

                //standardize other packed atts
                //Atts are always already unsigned.
                //Whether these are initially packed or standardizeed is not reliable.
                //  So if these are already float or doubles, assume already standardizeed
                if (destClass == float.class || destClass == double.class) {
                    String names[] = {"actual_min", "actual_max", "actual_range",
                                      "data_min",   "data_max",   "data_range",
                                      "valid_min",  "valid_max",  "valid_range"};
                    for (int i = 0; i < names.length; i++) {
                        PrimitiveArray pa = newAtts.get(names[i]);
                        if (pa != null && pa.elementClass() != float.class && pa.elementClass() != double.class) {
                            pa = PrimitiveArray.factory(destClass, pa);
                            pa.scaleAddOffset(scale, add);
                            newAtts.set(names[i], pa); 
                        }
                    }
                }

            } else {  //if not unsigned or packed, still need to convert missing values

                if (!Double.isNaN(dMissingValue) && dMissingValue != dataPa.missingValue()) {
                    dataPa.switchFromTo(       "" + dMissingValue, "");
                    if (debugMode) String2.log(
                        ">> #1: converted from missing_value=" + dMissingValue);
                }
                if (!Double.isNaN(dFillValue)    && dFillValue    != dataPa.missingValue()) {
                    dataPa.switchFromTo(       "" + dFillValue,    "");
                    if (debugMode) String2.log(
                        ">> #1: converted from _FillValue=" + dFillValue);
                }
            }

            //For all types other than String, need to specify _FillValue=standard missing value
            if (destClass != String.class)
                newAtts.add("_FillValue", PrimitiveArray.factory(destClass, 1, ""));
        }

        //if standardizeWhat & 2, convert numeric dateTimes to "seconds since 1970-01-01T00:00:00Z".
        if (standardizeNumericDateTimes) {
            //If #2 selected, #1 (unsigned, scale_factor, add_offset, missing_value, _FillValue) 
            //was automatically done above.
     
            //interpret tUnits
            double[] baseFactor = Calendar2.getTimeBaseAndFactor(oUnits); //throws exception

            //convert numeric time to epochSeconds (in DoubleArray)
            dataPa = Calendar2.unitsSinceToEpochSeconds(baseFactor[0], baseFactor[1], dataPa);
            newAtts.set("units", Calendar2.SECONDS_SINCE_1970);

            //convert mv, fv, and others to double and scale/addOffset
            String names[] = {"actual_min", "actual_max", "actual_range",
                              "data_min",   "data_max",   "data_range",
                              "valid_min",  "valid_max",  "valid_range",
                              "_FillValue", "missing_value"};
            for (int i = 0; i < names.length; i++) {
                PrimitiveArray pa = newAtts.remove(names[i]);
                if (pa != null) {
                    pa = new DoubleArray(pa);
                    pa.scaleAddOffset(baseFactor[1], baseFactor[0]);
                    newAtts.add(names[i], pa);
                }
            }

            if (debugMode) String2.log(
                ">> #2: converted numeric dateTimes (" + oUnits + ") to epochSeconds \"seconds since 1970-01-01T00:00:00Z\".");
        }

        //if standardizeWhat & 4, convert defined String _FillValue's and missing_value's to ""
        if ((standardizeWhat & 4) == 4 && dataPa.elementClass() == String.class) {
            String sFillValue    = oldAtts.getString("_FillValue");
            String sMissingValue = oldAtts.getString("missing_value");
            if (newAtts.remove("_FillValue")    != null)
                dataPa.switchFromTo(sFillValue, "");
            if (newAtts.remove("missing_value") != null)
                dataPa.switchFromTo(sMissingValue, "");
            if (debugMode) String2.log(
                ">> #4: converted defined String _FillValue's and missing_value's to \"\".");
        }

        //if standardizeWhat & 512, convert all common, undefined, missing value strings (e.g., "-", "N/A", "NONE", "?") and convert them to "".
        if ((standardizeWhat & 512) == 512 && dataPa.elementClass() == String.class) {
            int n = dataPa.size();
            String nothing = String2.canonical("");
            for (int i = 0; i < n; i++) {
                if (!String2.isSomething2(dataPa.getRawString(i))) 
                    dataPa.setString(i, nothing);
            }
            if (debugMode) String2.log(
                ">> #512: converted undefined missing value strings to \"\".");

        }

        //string dateTimes
        if ((standardizeWhat & (1024 + 2048)) != 0 && dataPa.elementClass() == String.class) {

            StringArray sa = (StringArray)dataPa;
            int n = sa.size();
            String firstS = null;
            for (int i = 0; i < n; i++) {
                if (String2.isSomething(sa.get(i))) {
                    firstS = sa.get(i);
                    break;
                }
            }

            boolean allDigits = false;
            String format = "";
            if (firstS == null) {
            } else {
                allDigits = String2.allDigits(firstS);
                format = Calendar2.suggestDateTimeFormat(sa, true); //evenIfPurelyNumeric
                //format will be "" if no match
            }


            //if standardizeWhat & 1024, try to convert not-purely-numeric String dateTimes 
            //  (e.g., "Jan 2, 2018") to ISO 8601 String dateTimes.
            if ((standardizeWhat & 1024) == 1024 && format.length() > 0 && !allDigits) {

                DoubleArray da = Calendar2.parseToEpochSeconds(sa, format); //NaN's if trouble
                sa.clear();
                //if source format has time, keep time in iso 8601 format (even if 00:00:00)
                if (format.indexOf('S') >= 0) {
                    //if source format has millis, keep time.millis in iso format (even if 00:00:00.000)
                    for (int i = 0; i < n; i++) 
                        sa.add(Calendar2.safeEpochSecondsToIsoStringT3Z(da.get(i), ""));
                    newAtts.set("units", Calendar2.ISO8601T3Z_FORMAT);
                } else if (format.indexOf('H') >= 0) {
                    //if source format has time, keep time in iso 8601 format (even if 00:00:00)
                    for (int i = 0; i < n; i++) 
                        sa.add(Calendar2.safeEpochSecondsToIsoStringTZ(da.get(i), ""));
                    newAtts.set("units", Calendar2.ISO8601TZ_FORMAT);
                } else {
                    //else just date
                    for (int i = 0; i < n; i++)
                        sa.add(Calendar2.safeEpochSecondsToIsoDateString(da.get(i), "")); 
                    newAtts.set("units", Calendar2.ISO8601DATE_FORMAT);
                }
                dataPa = sa;

                if (debugMode) String2.log(
                    ">> #1024: converted not-purely-numeric String dateTimes (e.g., \"Jan 2, 2018\") to ISO 8601 String dateTimes.");
            }

            //if standardizeWhat & 2048, try to convert purely-numeric String dateTimes 
            //  (e.g., "20180102") to ISO 8601 String dateTimes.
            if ((standardizeWhat & 2048) == 2048 && format.length() > 0 && allDigits) {

                DoubleArray da = Calendar2.parseToEpochSeconds(sa, format); //NaN's if trouble
                sa.clear();
                if (format.indexOf('S') >= 0) {
                    //if source format has millis, keep time.millis in iso 8601 format (even if 00:00:00.000)
                    for (int i = 0; i < n; i++) 
                        sa.add(Calendar2.safeEpochSecondsToIsoStringT3Z(da.get(i), ""));
                    newAtts.set("units", Calendar2.ISO8601T3Z_FORMAT);
                } else if (format.indexOf('H') >= 0) {
                    //if source format has time, keep time in iso 8601 format (even if 00:00:00)
                    for (int i = 0; i < n; i++) 
                        sa.add(Calendar2.safeEpochSecondsToIsoStringTZ(da.get(i), ""));
                    newAtts.set("units", Calendar2.ISO8601TZ_FORMAT);
                } else {
                    //else just date
                    for (int i = 0; i < n; i++)
                        sa.add(Calendar2.safeEpochSecondsToIsoDateString(da.get(i), "")); 
                    newAtts.set("units", Calendar2.ISO8601DATE_FORMAT);
                }
                dataPa = sa;

                if (debugMode) String2.log(
                    ">> #2048, converted purely-numeric String dateTimes (e.g., \"20180102\") to ISO 8601 String dateTimes.");
            }
        }

        //if standardizeWhat & 4096, try to standardize the units.
        if ((standardizeWhat & 4096) == 4096) {

            String tUnits = newAtts.getString("units");
            if (tUnits != null)
                //not ideal that this uses EDUnits, which uses EDStatic,
                //which is not in com.cohort.*.
                newAtts.set("units", gov.noaa.pfel.erddap.util.EDUnits.safeStandardizeUdunits(tUnits)); 

            if (debugMode) String2.log(
                ">> #4096, standardized units.");
        }


        return dataPa;
    }

    /**
     * If the variable is packed with scale_factor and/or add_offset, 
     *  this will unpack the packed attributes of the variable: 
     *   actual_range, actual_min, actual_max, 
     *   data_max, data_min, 
     *   valid_max, valid_min, valid_range.
     * missing_value and _FillValue will be converted to PA standard mv 
     *   for the the unpacked datatype (or current type if not packed).
     *
     * @param varName the var's fullName, for diagnostic messages only
     * @param oClass the var's initial elementClass
     */    
    public void unpackVariableAttributes(String varName, Class oClass) {

        Attributes newAtts = this;   //so it has a more descriptive name     
        Attributes oldAtts = new Attributes(this); //so we have an unchanged copy to refer to

        //deal with numeric time units
        String tUnits = newAtts.getString("units");
        if (Calendar2.isNumericTimeUnits(tUnits)) 
            newAtts.set("units", Calendar2.SECONDS_SINCE_1970); //AKA EDV.TIME_UNITS
            //presumably, String time var doesn't have units

        PrimitiveArray unsignedPA = newAtts.remove("_Unsigned");
        boolean unsigned = unsignedPA != null && "true".equals(unsignedPA.toString());         
        PrimitiveArray scalePA = newAtts.remove("scale_factor");
        PrimitiveArray addPA   = newAtts.remove("add_offset");

        //if present, convert _FillValue and missing_value to PA standard mv
        Class destClass = 
            scalePA != null? scalePA.elementClass() :
            addPA   != null? addPA.elementClass() : 
            unsigned && oClass == byte.class?  short.class :  //similar code below
            unsigned && oClass == short.class? int.class :
            unsigned && oClass == int.class?   double.class : //ints are converted to double because nc3 doesn't support long
            unsigned && oClass == long.class?  double.class : //longs are converted to double (not ideal)
            oClass;  
        if (newAtts.remove("_FillValue")    != null)
            newAtts.set(   "_FillValue",    PrimitiveArray.factory(destClass, 1, ""));
        if (newAtts.remove("missing_value") != null)
            newAtts.set(   "missing_value", PrimitiveArray.factory(destClass, 1, ""));

        //if var isn't packed, we're done
        if (!unsigned && scalePA == null && addPA == null)
            return; 

        //var is packed, so unpack all packed numeric attributes
        //lookForStringTimes is false because these are all attributes of numeric variables
        if (debugMode) 
            String2.log(">> before unpack " + varName + 
                " unsigned="      + unsigned +
                " scale_factor="  + scalePA + 
                " add_offset="    + addPA + 
                " actual_max="    + oldAtts.get("actual_max") + 
                " actual_min="    + oldAtts.get("actual_min") +
                " actual_range="  + oldAtts.get("actual_range") + 
                " data_max="      + oldAtts.get("data_max") + 
                " data_min="      + oldAtts.get("data_min") +
                " valid_max="     + oldAtts.get("valid_max") + 
                " valid_min="     + oldAtts.get("valid_min") +
                " valid_range="   + oldAtts.get("valid_range"));

        //attributes are never unsigned

        //before erddap v1.82, ERDDAP said actual_range had packed values.
        //but CF 1.7 says actual_range is unpacked. 
        //So look at data types and guess which to do, with preference to believing they're already unpacked
        //Note that at this point in this method, we're dealing with a packed data variable
        if (destClass != null && (destClass == float.class || destClass == double.class)) {
            PrimitiveArray pa;
            pa = newAtts.get("actual_max");
            if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
                newAtts.set("actual_max",   oldAtts.unpackPA(varName, pa, false, false)); 

            pa = newAtts.get("actual_min");
            if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
                newAtts.set("actual_min",   oldAtts.unpackPA(varName, pa, false, false)); 

            pa = newAtts.get("actual_range");
            if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
                newAtts.set("actual_range", oldAtts.unpackPA(varName, pa, false, false)); 

            pa = newAtts.get("data_min");
            if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
                newAtts.set("data_min",     oldAtts.unpackPA(varName, pa, false, false)); 

            pa = newAtts.get("data_max");
            if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
                newAtts.set("data_max",     oldAtts.unpackPA(varName, pa, false, false)); 
        }
        newAtts.set("valid_max",   oldAtts.unpackPA(varName, oldAtts.get("valid_max"),   false, false)); 
        newAtts.set("valid_min",   oldAtts.unpackPA(varName, oldAtts.get("valid_min"),   false, false));
        newAtts.set("valid_range", oldAtts.unpackPA(varName, oldAtts.get("valid_range"), false, false)); 

        if (debugMode) 
            String2.log(">> after  unpack " + varName + 
                " unsigned="      + unsigned +               
                " actual_max="    + newAtts.get("actual_max") + 
                " actual_min="    + newAtts.get("actual_min") +
                " actual_range="  + newAtts.get("actual_range") + 
                " data_max="      + newAtts.get("data_max") + 
                " data_min="      + newAtts.get("data_min") +
                " valid_max="     + newAtts.get("valid_max") + 
                " valid_min="     + newAtts.get("valid_min") +
                " valid_range="   + newAtts.get("valid_range"));
    }

   /**
     * Given this (sourceAtts, which won't be changed), this unpacks the dataPa.
     * If the var has time units, or scale_factor and add_offset, the values in the dataPa
     * will be unpacked (numeric times will be epochSeconds).
     * If missing_value and/or _FillValue are used, they are converted to PA standard mv.
     *
     * @param varName the var's fullName, for diagnostic messages only
     * @param dataPa  This may be an attribute's pa or a variable's dataPa.
     *    If null, no error and nothing is done.
     * @param dataPa This may be a dataVariable's pa, or one attribute's pa (e.g., valid_min).
     * @param lookForStringTimes If true, this tries to convert string times 
     *   (but not purely numeric formats because too many false positives).
     * @param lookForUnsigned If true, this looks for _Unsigned=true and unpacks dataPa.
     * @return The same dataPa or a new dataPa or null (if the dataPa parameter = null).
     *   missing_value and _FillValue will be converted to PA standard mv 
     *     for the the unpacked datatype (or current type if not packed).
     */
    public PrimitiveArray unpackPA(String varName, PrimitiveArray dataPa,
        boolean lookForStringTimes, boolean lookForUnsigned) {

        if (dataPa == null)
            return null;
        Attributes sourceAtts = this; //so it has a nice name
        Class dataPaClass = dataPa.elementClass();
        if (debugMode)
            String2.log(">> Attributes.unpackPA(var=" + varName);

        //any of these may be null
        PrimitiveArray scalePA = sourceAtts.get("scale_factor");
        PrimitiveArray addPA   = sourceAtts.get("add_offset");
        String tUnits          = sourceAtts.getString("units");         
        boolean unsigned       = lookForUnsigned && "true".equals(sourceAtts.getString("_Unsigned"));         
        //_FillValue and missing_value should be unsigned if _Unsigned=true, 
        //  but in practice sometimes aren't
        //and they should be packed if var is packed.
        //see EDDGridFromNcFilesUnpacked.testUInt16File()
        double dFillValue      = unsigned?
            sourceAtts.getUnsignedDouble("_FillValue") :   //so -1B becomes 255
            sourceAtts.getDouble(        "_FillValue");    //so e.g., 127 stays as 127
        double dMissingValue   = unsigned?
            sourceAtts.getUnsignedDouble("missing_value") ://so -1B becomes 255
            sourceAtts.getDouble(        "missing_value"); //so e.g., 127 stays as 127
        if (debugMode) String2.log(">> _FillValue=" + dFillValue + " missing_value=" + dMissingValue);

        //scale and add_offset -- done first, before check for numeric time
        if (unsigned || scalePA != null || addPA != null) {

            //figure out new data type indicated by scale_factor or add_offset 
            double scale = 1;
            double add = 0;
            Class tClass = null; //null is important, tests if set below
            if (scalePA != null) {  
                scale = scalePA.getNiceDouble(0);
                if (Double.isNaN(scale))
                    scale = 1;
                tClass = scalePA.elementClass();
            }
            if (addPA != null) {
                add = addPA.getNiceDouble(0);
                if (Double.isNaN(add))
                    add = 0;
                if (tClass == null)
                    tClass = addPA.elementClass();
            }
            if (tClass == null) //might be
                tClass = dataPaClass;
            //or data type needed by '_Unsigned'      //same code above
            if (unsigned && tClass == dataPaClass) {
                if      (tClass == byte.class)  tClass = short.class;
                else if (tClass == short.class) tClass = int.class;
                else if (tClass == int.class)   tClass = double.class; //ints are converted to doubles because nc3 doesn't support longs
                else if (tClass == long.class)  tClass = double.class; //longs are converted to doubles (not ideal)
            }

            //switch data type
            PrimitiveArray dataPa2 = 
                //if stated missing_value or _FillValue is same as cohort missingValue...
                unsigned? 
                    PrimitiveArray.unsignedFactory(tClass, dataPa) : //unsigned 
                    dataPa.isIntegerType() &&
                        (dMissingValue == dataPa.missingValue() ||  //e.g., 127 == 127
                         dFillValue    == dataPa.missingValue())?
                        PrimitiveArray.factory(        tClass, dataPa) : //missingValues (e.g., 127) are    changed, e.g., to NaN
                        PrimitiveArray.rawFactory(     tClass, dataPa);  //missingValues (e.g., 127) AREN'T changed
            if (debugMode) String2.log(
                ">> source="   + dataPaClass + ": " + dataPa.subset( 0, 1, Math.min(10, dataPa.size() -1)).toString() + "\n" +
                ">> dataPa2= " + tClass      + ": " + dataPa2.subset(0, 1, Math.min(10, dataPa2.size()-1)).toString());

            //dataPa2 is destination data type and now has (if relevant) unsigned values, 
            //but not yet scaleAddOffset.
            //If present, missing_value and _FillValue are packed
            //so convert other dMissingValue and dFillValue (e.g., -128) to PA standard mv
            //and apply before scaleAddOffset
            //String2.log("> dFillValue=" + dFillValue + " dataPa.missingValue=" + dataPa.missingValue());
            if (!Double.isNaN(dMissingValue) && dMissingValue != dataPa.missingValue()) {
                dataPa2.switchFromTo(      "" + dMissingValue, "");
                dMissingValue = Double.NaN;  //it's done
            }
            if (!Double.isNaN(dFillValue)    && dFillValue    != dataPa.missingValue()) {
                dataPa2.switchFromTo(      "" + dFillValue,    "");
                dFillValue = Double.NaN;     //it's done
            }

            //apply scaleAddOffset
            dataPa2.scaleAddOffset(scale, add); //it checks for (1,0)
            if (debugMode)
                String2.log(
                    ">> NcHelper.unpackPA applied scale_factor=" + scale + " add_offset=" + add + "\n" +
                    ">> unpacked=" + tClass + ": " + dataPa2.subset(0, 1, Math.min(10, dataPa2.size()-1)).toString());
            return dataPa2;

        }

        //numeric times (we know scale_factor and add_offset aren't used)
        if (Calendar2.isNumericTimeUnits(tUnits)) {
 
            //interpret tUnits
            double[] baseFactor = null;
            try {
                baseFactor = Calendar2.getTimeBaseAndFactor(tUnits); //throws exception
            } catch (Exception e) {
                String2.log(tUnits.toString());
                return PrimitiveArray.factory(double.class, dataPa.size(), "");   //i.e. uninterpretable
            }

            //switch data type
            PrimitiveArray dataPa2 = 
                //if stated missing_value or _FillValue is same as cohort missingValue...
                unsigned?
                    PrimitiveArray.unsignedFactory(double.class, dataPa) :
                    dataPa.isIntegerType() &&
                        (dMissingValue == dataPa.missingValue() ||  //e.g., 127 == 127
                         dFillValue    == dataPa.missingValue())?
                        PrimitiveArray.factory(        double.class, dataPa) : //missingValues (e.g., 127) are    changed
                        PrimitiveArray.rawFactory(     double.class, dataPa);  //missingValues (e.g., 127) AREN'T changed

            //convert other dMissingValue and dFillValue (e.g., -128)
            // before scaleAddOffset to epochSeconds
            if (!Double.isNaN(dMissingValue) && dMissingValue != dataPa.missingValue()) {
                dataPa2.switchFromTo(      "" + dMissingValue, "");
                dMissingValue = Double.NaN;  //it's done
            }
            if (!Double.isNaN(dFillValue)    && dFillValue    != dataPa.missingValue()) {
                dataPa2.switchFromTo(      "" + dFillValue,    "");
                dFillValue = Double.NaN;     //it's done
            }

            //convert numeric time to epochSeconds
            dataPa2 = Calendar2.unitsSinceToEpochSeconds(baseFactor[0], baseFactor[1], dataPa2);
            if (debugMode)
                String2.log(
                    ">> numeric time as epochSeconds: " + dataPa2.subset(0, 1, Math.min(10, dataPa2.size()-1)).toString());
            return dataPa2;
        } 
        
        //string times?  no units or not reliable units, so units aren't helpful. 
        if (lookForStringTimes && dataPa.elementClass() == String.class) {
            StringArray sa = (StringArray)dataPa;
            String format = Calendar2.suggestDateTimeFormat(sa, false);  //false for evenIfPurelyNumeric
            if (format.length() > 0) {
                if (verbose) String2.log("  " + varName + " has String times format=" + format);
                dataPa = Calendar2.parseToEpochSeconds(sa, format);
            }
            return dataPa;
        }

        //none of the above situations apply 
        //  (i.e., not unsigned, and no scale_factor or add_offset, not numeric or string time)
        //still need to convert missingValue to dataPa missing value
        if (!Double.isNaN(dMissingValue) && dMissingValue != dataPa.missingValue()) {
            dataPa.switchFromTo(       "" + dMissingValue, "");
            dMissingValue = Double.NaN;  //it's done
        }
        if (!Double.isNaN(dFillValue)    && dFillValue    != dataPa.missingValue()) {
            dataPa.switchFromTo(       "" + dFillValue,    "");
            dFillValue = Double.NaN;     //it's done
        }

        return dataPa;
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Exception if trouble
     */
    public static void test() throws Exception {
        String2.log("\n*** Attributes.test()");
/* for releases, this line should have open/close comment */

        //set  and size
        Attributes atts = new Attributes();
        String results;
        Test.ensureEqual(atts.size(), 0, "");
        atts.set("byte", (byte)1);
        atts.set("char", 'a');
        atts.set("short", (short)3000);
        atts.set("int",  1000000);
        atts.set("long", 1000000000000L);
        atts.set("float", 2.5f);
        atts.set("double", Math.PI);
        atts.set("String", "a, csv, string");
        atts.set("PA", new IntArray(new int[]{1,2,3}));

        Test.ensureEqual(atts.size(), 9, "");

        //add and remove an item
        Test.ensureEqual(atts.set("zz", new IntArray(new int[]{1})), null, "");
        Test.ensureEqual(atts.size(), 10, "");

        Test.ensureEqual(atts.set("zz", (PrimitiveArray)null), new IntArray(new int[]{1}), "");
        Test.ensureEqual(atts.size(), 9, "");

        //add and remove an item
        Test.ensureEqual(atts.set("zz", new IntArray(new int[]{2})), null, "");
        Test.ensureEqual(atts.size(), 10, "");

        Test.ensureEqual(atts.remove("zz"), new IntArray(new int[]{2}), "");
        Test.ensureEqual(atts.size(), 9, "");

        //empty string same as null; attribute removed
        atts.set("zz", "a"); 
        Test.ensureEqual(atts.size(), 10, "");
        atts.set("zz", ""); 
        Test.ensureEqual(atts.size(), 9, "");

        //get
        Test.ensureEqual(atts.get("byte"),   new ByteArray(new byte[]{(byte)1}), "");
        Test.ensureEqual(atts.get("char"),   new CharArray(new char[]{'a'}), "");
        Test.ensureEqual(atts.get("short"),  new ShortArray(new short[]{(short)3000}), "");
        Test.ensureEqual(atts.get("int"),    new IntArray(new int[]{1000000}), "");
        Test.ensureEqual(atts.get("long"),   new LongArray(new long[]{1000000000000L}), "");
        Test.ensureEqual(atts.get("float"),  new FloatArray(new float[]{2.5f}), "");
        Test.ensureEqual(atts.get("double"), new DoubleArray(new double[]{Math.PI}), "");
        Test.ensureEqual(atts.get("String"), new StringArray(new String[]{"a, csv, string"}), "");
        Test.ensureEqual(atts.get("PA"),     new IntArray(new int[]{1,2,3}), "");

        Test.ensureEqual(atts.getInt("byte"),  1, "");
        Test.ensureEqual(atts.getInt("char"),  97, "");
        Test.ensureEqual(atts.getInt("short"), 3000, "");
        Test.ensureEqual(atts.getInt(   "int"), 1000000, "");
        Test.ensureEqual(atts.getLong(  "int"), 1000000, "");
        Test.ensureEqual(atts.getString("int"), "1000000", "");
        Test.ensureEqual(atts.getLong(  "long"), 1000000000000L, "");
        Test.ensureEqual(atts.getInt(   "long"), Integer.MAX_VALUE, "");
        Test.ensureEqual(atts.getFloat( "float"), 2.5f, "");
        Test.ensureEqual(atts.getDouble("float"), 2.5f, "");
        Test.ensureEqual(atts.getString("float"), "2.5", "");
        Test.ensureEqual(atts.getDouble("double"), Math.PI, "");
        Test.ensureEqual(atts.getInt(   "double"), 3, "");
        Test.ensureEqual(atts.getString(        "String"), "a, csv, string", "");
        Test.ensureEqual(atts.getStringsFromCSV("String"), new String[]{"a", "csv", "string"}, "");
        Test.ensureEqual(atts.getInt(           "String"), Integer.MAX_VALUE, "");
        Test.ensureEqual(atts.get(      "PA"), new IntArray(new int[]{1,2,3}), "");
        Test.ensureEqual(atts.getInt(   "PA"), 1, "");
        Test.ensureEqual(atts.getDouble("PA"), 1, "");
        Test.ensureEqual(atts.getString("PA"), "1", "");

        //getNames
        Test.ensureEqual(atts.getNames(), 
            new String[]{"byte", "char", "double", "float", "int", "long", "PA", "short", "String"}, 
            "");

        //toString
        results = atts.toString();
        Test.ensureEqual(results, 
            "    byte=1b\n" +
            "    char=\"'a'\"\n" +
            "    double=3.141592653589793d\n" +
            "    float=2.5f\n" +
            "    int=1000000i\n" +
            "    long=1000000000000L\n" +
            "    PA=1i,2i,3i\n" +   
            "    short=3000s\n" +
            "    String=\"a, csv, string\"\n", "results=\n" + results);

        //clone   
        Attributes atts2 = (Attributes)atts.clone();
        Test.ensureEqual(atts2.get("byte"),   new ByteArray(new byte[]{(byte)1}), "");
        Test.ensureEqual(atts2.get("char"),   new CharArray(new char[]{'a'}), "");
        Test.ensureEqual(atts2.get("short"),  new ShortArray(new short[]{(short)3000}), "");
        Test.ensureEqual(atts2.get("int"),    new IntArray(new int[]{1000000}), "");
        Test.ensureEqual(atts2.get("long"),   new LongArray(new long[]{1000000000000L}), "");
        Test.ensureEqual(atts2.get("float"),  new FloatArray(new float[]{2.5f}), "");
        Test.ensureEqual(atts2.get("double"), new DoubleArray(new double[]{Math.PI}), "");
        Test.ensureEqual(atts2.get("String"), new StringArray(new String[]{"a, csv, string"}), "");
        Test.ensureEqual(atts2.get("PA"),     new IntArray(new int[]{1,2,3}), "");

        Test.ensureEqual(atts2.getNames(), 
            new String[]{"byte", "char", "double", "float", "int", "long", "PA", "short", "String"},
            "");
        
        //clear
        atts2.clear();
        Test.ensureEqual(atts2.getNames(), new String[]{}, "");

        //copyTo
        atts.copyTo(atts2);
        Test.ensureEqual(atts2.getNames(), 
            new String[]{"byte", "char", "double", "float", "int", "long", "PA", "short", "String"},
            "");
        Test.ensureEqual(atts2.get("String"), new StringArray(new String[]{"a, csv, string"}), "");

        //equals
        Test.ensureTrue(atts.equals(atts2), "");

        //add
        Attributes atts3 = (new Attributes()).add("byte", (byte)1)
            .add("char", 'a')
            .add("short", (short)3000)
            .add("int",  1000000)
            .add("long", 1000000000000L)
            .add("float", 2.5f)
            .add("double", Math.PI)
            .add("String", "a, csv, string")
            .add("PA", new IntArray(new int[]{1,2,3}));
        Test.ensureTrue(atts3.equals(atts), "");

        //set(attributes)
        Attributes atts4 = (new Attributes()).add("zztop", 77) //new name
            .add("char", 'd')   //diff value
            .add("short", (short)3000); //same value
        atts3.set(atts4);
        results = atts3.toString();
        Test.ensureEqual(results, 
            "    byte=1b\n" +
            "    char=\"'d'\"\n" +
            "    double=3.141592653589793d\n" +
            "    float=2.5f\n" +
            "    int=1000000i\n" +
            "    long=1000000000000L\n" +
            "    PA=1i,2i,3i\n" +   
            "    short=3000s\n" +
            "    String=\"a, csv, string\"\n" +
            "    zztop=77i\n",
            "results=\n" + results);

        //_FillValue
        atts.clear();
        atts.set("_FillValue", new ShortArray(new short[]{(short)32767}));
        String2.log("atts.size()=" + atts.size());
        PrimitiveArray fv = atts.get("_FillValue");
        if (fv == null || fv.size() != 1 || !(fv instanceof ShortArray))
            throw new Exception("fv=" + fv);
        atts.remove("_FillValue");
        
        //keepIfDifferent
        atts.clear();
        atts.add("a", 1);
        atts.add("b", "2");
        atts.add("c", 3f);
        atts2.clear();
        atts2.add("b", "2");
        atts2.add("c", 3d); //changed to d
        atts2.add("d", 4d);
        atts.removeIfSame(atts2);
        Test.ensureEqual(atts.toString(), 
            "    a=1i\n" +
            "    c=3.0f\n", //different type
            "");

        //trim
        atts.clear();
        atts.add(" a ", " A ");
        atts.add("b ", "B");
        atts.add("c",  "C");
        atts.add("d",  4);
        atts.trim();
        results = atts.toString();
        Test.ensureEqual(results, 
            "    a=A\n" +
            "    b=B\n" +
            "    c=C\n" +
            "    d=4i\n",
            "results=\n" + results);

        //trimIfNeeded
        atts.clear();
        atts.add("a",   " A ");
        atts.add("b",   "B");
        atts.add("c",   " C ");
        atts.add("d",   "D");
        atts.add("z4",  4);
        atts2.clear();
        atts.add("a",   "A");
        atts.add("b",   " B ");
        atts.add("e",   " E ");
        atts.add("f",   "F");
        atts.add("z5",  5);
        atts.trimIfNeeded(atts2);
        results = atts.toString();
        Test.ensureEqual(results, 
            "    a=A\n" +
            "    b=B\n" +
            "    c=C\n" +
            "    d=D\n" +
            "    e=E\n" +
            "    f=F\n" +
            "    z4=4i\n" +
            "    z5=5i\n",
            "results=\n" + results);

        //makeALikeB
        Attributes a =  new Attributes();
        a.set("s",      new StringArray(new String[]{"theString"})); //same
        a.set("number", new DoubleArray(new double[]{11, 22}));      //different
        a.set("inA",    new IntArray(   new int[]{1, 2}));           //unique 
        Attributes b =  new Attributes();
        b.set("s",      new StringArray(new String[]{"theString"})); //same
        b.set("number", new IntArray(   new int[]{11, 22}));         //different
        b.set("inB",    new IntArray(   new int[]{3, 4, 5}));        //unique

        atts = makeALikeB(a, b);
        Test.ensureEqual(atts.toString(), 
"    inA=\"null\"\n" +
"    inB=3i,4i,5i\n" +
"    number=11i,22i\n",
            "atts=\n" + atts.toString());
        a.add(atts);
        a.removeValue("\"null\"");
        Test.ensureEqual(a, b, "");
        Test.ensureEqual(a.toString(), b.toString(), "");


        //***** PrimitiveArray standardizeVariable(int standardizeWhat, String varName, PrimitiveArray dataPa) 
        PrimitiveArray pa;

        //standardizeWhat = 1: _Unsigned byte -> short
        atts.clear();
        atts.set("_Unsigned", "true");  
        atts.set("data_min", (byte)0); 
        atts.set("missing_value", (byte)99);
        pa = PrimitiveArray.csvFactory(byte.class, "-128, -1, 0, 1, 99, 127");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "128, 255, 0, 1, 32767, 127", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=32767s\n" +
"    data_min=0b\n", "");  

        //standardizeWhat = 1: _Unsigned int -> long -> double 
        atts.clear();
        atts.set("_Unsigned", "true");  
        atts.set("data_min", 0.0); 
        atts.set("missing_value", 99);
        pa = PrimitiveArray.csvFactory(int.class, "-2000000000, -1, 0, 1, 99, 2000000000");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "2.294967296E9, 4.294967295E9, 0.0, 1.0, NaN, 2.0E9", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNd\n" +
"    data_min=0.0d\n", "");  

        //standardizeWhat = 1: scale_factor
        //if scale_factor is defined (e.g., in .nc file), mv's should be too. If not, then all values are valid.
        atts.clear();
        atts.set("data_min", (short)-2); 
        atts.set("scale_factor", .01f);  //float
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "-0.02, 0.0, 3.0, 327.67", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNf\n" +
"    data_min=-0.02f\n", "");  //float

        //standardizeWhat = 1: scale_factor
        atts.clear();
        atts.set("data_min", (short)-2); 
        atts.set("scale_factor", .01); //double
        atts.set("missing_value", 32767);
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "-0.02, 0.0, 3.0, NaN", "");
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNd\n" +
"    data_min=-0.02d\n", ""); //double

        //standardizeWhat = 1: add_offset
        atts.clear();
        atts.set("add_offset", 10f);
        atts.set("data_min", (short)-2); 
        atts.set("missing_value", 32767);
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "8.0, 10.0, 310.0, NaN", "");
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNf\n" +
"    data_min=8.0f\n", "");

        //standardizeWhat = 1: scale_factor, add_offset
        atts.clear();
        atts.set("add_offset", .10f);
        atts.set("data_min", (short)-2); 
        atts.set("missing_value", (short)300);
        atts.set("scale_factor", .01f);
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "0.08, 0.1, NaN, 327.77", "");
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNf\n" +
"    data_min=0.08f\n", "");

        //standardizeWhat = 1: missing_value 
        atts.clear();
        atts.set("data_min", (short)-2); 
        atts.set("missing_value", (short)300); 
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "-2, 0, 32767, 32767", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=32767s\n" + //not missing_value
"    data_min=-2s\n", "");  

        //standardizeWhat = 1: _FillValue 
        atts.clear();
        atts.set("data_min", (short)-2); 
        atts.set("_FillValue", (short)300); 
        pa = PrimitiveArray.csvFactory(double.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "-2.0, 0.0, NaN, 32767.0", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNd\n" +
"    data_min=-2s\n", "");  //unchanged since data wasn't unpacked

        //standardizeWhat = 1: _FillValue and missing_value
        atts.clear();
        atts.set("_FillValue", 300); 
        atts.set("actual_min", (short)-2); 
        atts.set("missing_value", 32767); 
        pa = PrimitiveArray.csvFactory(double.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1, "test", pa);
        Test.ensureEqual(pa.toString(), "-2.0, 0.0, NaN, NaN", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNd\n" +
"    actual_min=-2s\n", "");  //unchanged since data wasn't unpacked


        //standardizeWhat = 2: convert defined numeric dateTimes to epochSeconds
        //days since 1900
        atts.clear();
        atts.set("_FillValue", (short)32767); 
        atts.set("actual_min", (short)-2); 
        atts.set("units", "days since 1900-1-1"); 
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1 + 2, "test", pa);
        Test.ensureEqual(pa.toString(), "-2.2091616E9, -2.2089888E9, -2.1830688E9, NaN", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNd\n" +
"    actual_min=-2.2091616E9d\n" +  //related atts are converted
"    units=seconds since 1970-01-01T00:00:00Z\n", "");  

        //standardizeWhat = 2: convert defined numeric dateTimes to epochSeconds
        //days since 1900
        atts.clear();
        atts.set("data_min", (short)-2); 
        atts.set("missing_value", (short)300);   //different value
        atts.set("units", "days since 1900-1-1"); 
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 32767");
        pa = atts.standardizeVariable(1 + 2, "test", pa);
        Test.ensureEqual(pa.toString(), "-2.2091616E9, -2.2089888E9, NaN, NaN", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNd\n" + //always just _FillValue
"    data_min=-2.2091616E9d\n" +  //related atts are converted
"    units=seconds since 1970-01-01T00:00:00Z\n", "");  


        //standardizeWhat = 4: convert defined String mv to ""
        atts.clear();
        atts.set("_FillValue", "QQQ"); 
        pa = PrimitiveArray.csvFactory(String.class, "a, bb, , QQQ, none");
        pa = atts.standardizeVariable(4, "test", pa);
        Test.ensureEqual(pa.toString(), "a, bb, , , none", ""); 
        Test.ensureEqual(atts.toString(), "", "");  


        //standardizeWhat=256: if dataPa is numeric, try to identify an undefined numeric missing_value (-999?, 9999?, 1e37f?) 
        atts.clear();
        atts.set("data_min", (short)-2); 
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, 9999");
        pa = atts.standardizeVariable(256 + 1, "test", pa);
        Test.ensureEqual(pa.toString(), "-2, 0, 300, 32767", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=32767s\n" +
"    data_min=-2s\n", "");  //unchanged since dataPa wasn't packed

        //standardizeWhat=1+256  scale_factor and find 99999
        //if scale_factor is defined (e.g., in .nc file), mv's should be too. If not, then all values are valid.
        atts.clear();
        atts.set("data_min", (short)-2); 
        atts.set("scale_factor", 100f);  //float
        pa = PrimitiveArray.csvFactory(int.class, "-2, 0, 300, 999");
        pa = atts.standardizeVariable(1 + 256, "test", pa);
        Test.ensureEqual(pa.toString(), "-200.0, 0.0, 30000.0, 2.14748365E11", "");  
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNf\n" +
"    data_min=-200.0f\n", "");  //float


        //standardizeWhat=256: if dataPa is numeric, try to identify an undefined numeric missing_value (-999?, 9999?, 1e37f?) 
        atts.clear();
        atts.set("data_min", (short)-2); 
        pa = PrimitiveArray.csvFactory(short.class, "-2, 0, 300, -99");
        pa = atts.standardizeVariable(256 + 1, "test", pa);
        Test.ensureEqual(pa.toString(), "-2, 0, 300, 32767", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=32767s\n" +
"    data_min=-2s\n", "");  

        atts.clear();
        atts.set("data_min", -2.0f); 
        pa = PrimitiveArray.csvFactory(float.class, "-2, 0, 300, 1.1e37");
        pa = atts.standardizeVariable(256 + 1, "test", pa);
        Test.ensureEqual(pa.toString(), "-2.0, 0.0, 300.0, NaN", ""); 
        Test.ensureEqual(atts.toString(), 
"    _FillValue=NaNf\n" +
"    data_min=-2.0f\n", "");  

        atts.clear();
        atts.set("data_min", (short)-2); 
        pa = PrimitiveArray.csvFactory(short.class, "-20000, -9999, 0, 20000, 9999");
        pa = atts.standardizeVariable(256, "test", pa);
        Test.ensureEqual(pa.toString(), "-20000, -9999, 0, 20000, 9999", ""); // +/-9999 isn't max, so isn't found
        Test.ensureEqual(atts.toString(), 
"    data_min=-2s\n", "");  

        atts.clear();
        atts.set("data_min", (short)-2); 
        pa = PrimitiveArray.csvFactory(short.class, "-20000, -9999, 0, 20000, 9999");
        pa = atts.standardizeVariable(256 + 1, "test", pa);  //256 + 1
        Test.ensureEqual(pa.toString(), "-20000, -9999, 0, 20000, 9999", ""); // +/-9999 isn't max, so isn't found
        Test.ensureEqual(atts.toString(), 
"    _FillValue=32767s\n" +   //standardizeWhat=1 adds this
"    data_min=-2s\n", "");  


        //standardizeWhat=512: if dataPa is Strings, convert all common, undefined, missing value strings (e.g., "n/a") to ""
        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "a, bb, , n/a");
        pa = atts.standardizeVariable(512, "test", pa);
        Test.ensureEqual(pa.toString(), "a, bb, , ", ""); 
        Test.ensureEqual(atts.toString(), "", "");  



        //standardizeWhat=1024: if dataPa is Strings, try to convert not-purely-numeric String dateTimes to ISO 8601 String
        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "\"Jan 2, 1970\", , \"DEC 32, 2000\""); //forgiving
        pa = atts.standardizeVariable(1024, "test", pa);
        Test.ensureEqual(pa.toString(), 
"1970-01-02, , 2001-01-01", ""); 
        Test.ensureEqual(atts.toString(), 
"    units=yyyy-MM-dd\n", "");  

        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "\"Jan 2, 1970 00:00:00\", , \"DEC 32, 2000 01:02:03\""); //forgiving
        pa = atts.standardizeVariable(1024, "test", pa);
        Test.ensureEqual(pa.toString(), 
"1970-01-02T00:00:00Z, , 2001-01-01T01:02:03Z", ""); 
        Test.ensureEqual(atts.toString(), 
"    units=yyyy-MM-dd'T'HH:mm:ssZ\n", "");  

        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "\"Jan 2, 1970 00:00:00.0\", , \"DEC 32, 2000 01:02:03.4\""); //forgiving
        pa = atts.standardizeVariable(1024, "test", pa);
        Test.ensureEqual(pa.toString(), 
"1970-01-02T00:00:00.000Z, , 2001-01-01T01:02:03.400Z", ""); 
        Test.ensureEqual(atts.toString(), 
"    units=yyyy-MM-dd'T'HH:mm:ss.SSSZ\n", "");  

        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "\"Jan 2, 1970 00:00:00.0\", , \"DEC 32 , 2000 01:02:03.4\""); 
        pa = atts.standardizeVariable(1024, "test", pa);    //extra space before comma not matched
        Test.ensureEqual(pa.toString(), 
"\"Jan 2, 1970 00:00:00.0\", , \"DEC 32 , 2000 01:02:03.4\"", "");  //unchanged
        Test.ensureEqual(atts.toString(), "", "");  


        //standardizeWhat=2048: if dataPa is Strings, try to convert purely-numeric String dateTimes to ISO 8601 Strings
        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "\"19700102\", , \"20001231\""); //not forgiving, so don't try
        pa = atts.standardizeVariable(2048, "test", pa);
        Test.ensureEqual(pa.toString(), 
"1970-01-02, , 2000-12-31", ""); 
        Test.ensureEqual(atts.toString(), 
"    units=yyyy-MM-dd\n", "");  

        atts.clear();
        pa = PrimitiveArray.csvFactory(String.class, "\"19700102\", , \"20001240\""); // day 40 won't be matched
        pa = atts.standardizeVariable(2048, "test", pa);
        Test.ensureEqual(pa.toString(), 
"19700102, , 20001240", "");  //unchanged
        Test.ensureEqual(atts.toString(), "", "");  



        String2.log("*** test Attributes finished successfully.");
    } 

}
