/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.*;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds a list of attributes (name=value, where name is a String and value is a
 * PrimitiveArray). The backing datastructure (ConcurrentHashMap) is thread-safe.
 *
 * @author Bob Simons (CoHortSoftware@gmail.com)
 */
public class Attributes {
  // FUTURE: implement as ArrayString for names and ArrayList for values?

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean debugMode = false;

  /** The backing data structure. It is thread-safe. */
  private ConcurrentHashMap<String, PrimitiveArray> hashmap = new ConcurrentHashMap(16, 0.75f, 4);

  public static String signedToUnsignedAttNames[] =
      new String[] {
        "_FillValue",
        "actual_max",
        "actual_min",
        "actual_range",
        "data_max",
        "data_min",
        "data_range",
        "missing_value",
        "valid_max",
        "valid_min",
        "valid_range"
      };

  /** This constructs a new, empty Attributes object. */
  public Attributes() {}

  /**
   * This constructs a new Attributes object which is a clone of 'attributes'.
   *
   * @param tAtts a set of attributes, each of which will be cloned to make a new Attributes object.
   */
  public Attributes(Attributes tAtts) {
    if (tAtts == null) String2.log("WARNING! tAtts is null in Attributes(tAtts)."); // do nothing
    else tAtts.copyTo(this);
  }

  /**
   * This constructs a new Attributes object which has the contents of moreImportant and
   * lessImportant.
   *
   * @param moreImportant the basis for a new Attributes. If one of the keys is the same as in
   *     lessImportant, the value from this takes precedence.
   * @param lessImportant the basis for a new Attributes.
   */
  public Attributes(Attributes moreImportant, Attributes lessImportant) {
    lessImportant.copyTo(this);
    set(moreImportant); // duplicate keys in 'more' will overwrite keys from 'less'
  }

  /** This clears all the attributes being held. The result isValid(). */
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
  @Override
  public Object clone() {
    Attributes attributes2 = new Attributes();
    this.copyTo(attributes2);
    return attributes2;
  }

  /**
   * This returns the value of a specific attribute (or null if the name isn't defined).
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
   * @return an array with the names of all of the attributes, sorted in a case-insensitive way.
   */
  public String[] getNames() {
    StringArray names = new StringArray(hashmap.keys());
    names.sortIgnoreCase();
    return names.toArray();
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a String, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the String attribute or null if trouble (e.g., not found). This uses pa.getRawString(0)
   *     so e.g., 127 in ByteArray will appear as "127", and NaN in DoubleArray will appear as
   *     "NaN".
   */
  public String getString(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return null;
      return pa.getRawString(0);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * A convenience method which assumes the first element of the attribute's value PrimitiveArray is
   * a CSV String and which splits the string into parts.
   *
   * @param name the name of an attribute
   * @return a String[] or null if trouble (e.g., not found)
   */
  public String[] getStringsFromCSV(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return null;
      String csv = pa.getRawString(0);
      return StringArray.arrayFromCSV(csv);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Abe").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a PAOne which has the value associated with the 'name'. If value isn't null, this
   *     adds value.pa().clone(). If value is null, name is removed from attributes.
   * @return 'this'
   */
  public Attributes add(String name, PAOne value) {
    set(name, value == null ? null : value.pa().clone());
    return this;
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a PAOne.
   *
   * @param name the name of an attribute
   * @return the attribute as a PAOne (or null if trouble (e.g., not found))
   */
  public PAOne getPAOne(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return null;
      return new PAOne(pa, 0);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a double, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
   */
  public double getDouble(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Double.NaN;
      return pa.getDouble(0);
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a double, regardless of the type used to store it. In this "unsigned" variant, if pa
   * isIntegerType, then the value is interpreted as an unsigned value (two's compliment, e.g., -1b
   * becomes 255). Note that the native cohort missing value (e.g., 127b) is treated as a value, not
   * a missing value.
   *
   * @param name the name of an attribute
   * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
   */
  public double getUnsignedDouble(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Double.NaN;
      return pa.getUnsignedDouble(0);
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a double, regardless of the type used to store it. In this "raw" variant, if pa isIntegerType,
   * then the cohort missingValue (e.g., ByteArray missingValue=127) is left intact and NOT
   * converted to new pa's missingValue (e.g., Double.NaN).
   *
   * @param name the name of an attribute
   * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
   */
  public double getRawDouble(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Double.NaN;
      return pa.getRawDouble(0);
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a double, regardless of the type used to store it. If the type was float, this returns
   * Math2.floatToDouble(value).
   *
   * @param name the name of an attribute
   * @return the attribute as a nice double or Double.NaN if trouble (e.g., not found). E.g.,
   *     ByteArray 127 will be returned as Double.NaN.
   */
  public double getNiceDouble(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Double.NaN;
      return pa.getNiceDouble(0);
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a float, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as a float or Float.NaN if trouble (e.g., not found)
   */
  public float getFloat(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Float.NaN;
      return pa.getFloat(0);
    } catch (Exception e) {
      return Float.NaN;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a long, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as a long or Long.MAX_VALUE if trouble (e.g., not found)
   */
  public long getLong(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Long.MAX_VALUE;
      return pa.getLong(0);
    } catch (Exception e) {
      return Long.MAX_VALUE;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a ulong, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as a ulong or null if trouble (e.g., not found). This acts like
   *     maxIsMV=true and returns MAX_VALUE.
   */
  public BigInteger getULong(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return ULongArray.MAX_VALUE;
      BigInteger bi = pa.getULong(0);
      return bi == null ? ULongArray.MAX_VALUE : bi;
    } catch (Exception e) {
      return ULongArray.MAX_VALUE;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * an int, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as an int or Integer.MAX_VALUE if trouble (e.g., not found)
   */
  public int getInt(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Integer.MAX_VALUE;
      return pa.getInt(0);
    } catch (Exception e) {
      return Integer.MAX_VALUE;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * an int, regardless of the type used to store it. In this "raw" variant, if pa isIntegerType,
   * then the cohort missingValue (e.g., ByteArray missingValue=127) is left intact and NOT
   * converted to new pa's missingValue (e.g., Double.NaN).
   *
   * @param name the name of an attribute
   * @return the attribute as an int or Integer.MAX_VALUE if trouble (e.g., not found)
   */
  public int getRawInt(String name) {
    try {
      PrimitiveArray pa = get(name);
      if (pa == null || pa.size() == 0) return Integer.MAX_VALUE;
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
   * This is the main method to set the value of a specific attribute (adding it if it doesn't
   * exist, revising it if it does, or removing it if value is (PrimitiveArray)null).
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'. If value is null
   *     or size==0 or it is one String="", name is removed from attributes.
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, PrimitiveArray value) {
    if (value == null
        || value.size() == 0
        || (value.size() == 1
            && value.elementType() == PAType.STRING
            && value.getRawString(0).trim().length() == 0)) return hashmap.remove(name);
    return hashmap.put(String2.canonical(name), value);
  }

  /**
   * Like set, but only sets the value if there is no current value.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'. If value is null
   *     or "", name is removed from attributes.
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray setIfNotAlreadySet(String name, PrimitiveArray value) {
    PrimitiveArray pa = get(name);
    if (pa != null) return pa;
    return set(name, value);
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'. If value is null,
   *     name is removed from attributes.
   * @return 'this'
   */
  public Attributes add(String name, PrimitiveArray value) {
    set(name, value);
    return this;
  }

  /**
   * This calls set() for all the attributes in 'additional'. In case of duplicate names, the
   * 'additional' attributes have precedence.
   *
   * @param moreImportant the Attributes to be added.
   */
  public void set(Attributes moreImportant) {
    Enumeration en = moreImportant.hashmap.keys();
    while (en.hasMoreElements()) {
      String name = (String) en.nextElement();
      set(name, (PrimitiveArray) moreImportant.get(name).clone());
    }
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
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
   * @param value a Byte, Short, ... String. If value is null or "", name is removed from
   *     attributes.
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, Object value) {
    if (value == null) return remove(name);
    return set(name, PrimitiveArray.factory(value));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a String which is the value associated with the 'name'
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
   * @param value a PrimitiveArray which is the value associated with the 'name'. If value is null
   *     or "", name is removed from attributes.
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, String value) {
    // netCDF doesn't allow 0 length strings
    if (value == null || value.trim().length() == 0) return remove(name);
    return set(
        name, new StringArray(new String[] {value})); // new StringArray calls String2.canonical
  }

  /**
   * Like set, but only sets the value if there is no current value.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'. If value is null
   *     or "", name is removed from attributes.
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray setIfNotAlreadySet(String name, String value) {
    PrimitiveArray pa = get(name);
    if (pa != null) return pa;
    return set(name, value);
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a String which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, String value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the double in a DoubleArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, double value) {
    return set(name, new DoubleArray(new double[] {value}));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a double which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, double value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the float in a FloatArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, float value) {
    return set(name, new FloatArray(new float[] {value}));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a float which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, float value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the long in an LongArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, long value) {
    return set(name, new LongArray(new long[] {value}));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a long which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, long value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the int in an IntArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, int value) {
    return set(name, new IntArray(new int[] {value}));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value an int which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, int value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the short in an ShortArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, short value) {
    return set(name, new ShortArray(new short[] {value}));
  }

  /**
   * A convenience method which changes the name of an attribute.
   *
   * @param oldName the current name of the attribute to be changed
   * @param newName the new name for the attribute
   * @return the PrimitiveArray with the attribute's value (will be null if oldName doesn't exist)
   */
  public PrimitiveArray changeName(String oldName, String newName) {
    PrimitiveArray pa = remove(oldName);
    if (pa != null) return null;
    add(newName, pa);
    return pa;
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a short which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, short value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the char in an CharArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, char value) {
    return set(name, new CharArray(new char[] {value}));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a char which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, char value) {
    set(name, value);
    return this;
  }

  /**
   * A convenience method which stores the byte in an ByteArray then stores the attribute.
   *
   * @param name the name of the attribute
   * @param value a PrimitiveArray which is the value associated with the 'name'
   * @return the previous value stored for attributeName, or null if none
   */
  public PrimitiveArray set(String name, byte value) {
    return set(name, new ByteArray(new byte[] {value}));
  }

  /**
   * This is like the similar set() method, but returns 'this'. add() lets you string several set
   * commands together, e.g., (new Attributes()).add("name", "Bob").add("height", 197);
   *
   * @param name the name of the attribute
   * @param value a byte which is the value associated with the 'name'
   * @return 'this'
   */
  public Attributes add(String name, byte value) {
    set(name, value);
    return this;
  }

  /**
   * For diagnostics, this prints the attributes to a newline separated String, one per line:
   * "&nbsp;&nbsp;&nbsp;&nbsp;[name]=[value]", where [value] is att.toNccsv127AttString().
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    String names[] = getNames();
    for (int i = 0; i < names.length; i++) {
      sb.append("    " + names[i] + "=" + get(names[i]).toNccsv127AttString() + "\n");
    }
    return sb.toString();
  }

  /**
   * This removes any entry which has a String value of 'value'. value must equal the pa.toString()
   * of the attribute's value, so a string with the word null must be requesth here as "\"null\""
   *
   * @param value Any attribute that has this value (when evaluated as a String) will be removed.
   */
  public void removeValue(String value) {
    Iterator it =
        hashmap.keySet().iterator(); // iterator (not enumeration) since I use it.remove() below
    while (it.hasNext()) {
      String name = (String) it.next();
      // String2.log(">> lookFor=" + value + " found=" + get(name).toString());
      if (get(name).toString().equals(value)) it.remove();
    }
  }

  /**
   * This generates a String with "[prefix][name] = [value][suffix]" on each line.
   *
   * <p>This nc-style version is used to print netcdf header attributes. It uses String2.toJson for
   * String attributes.
   *
   * @param prefix The text to be precede "[name] = [value]" on each line, perhaps "".
   * @param suffix The text to follow "[name] = [value]" on each line, perhaps "".
   * @return the desired string representation
   */
  public String toNcString(String prefix, String suffix) {
    // <p>Was: It puts quotes around String attributeValues,
    // converts \ to \\, and " to \", and appends 'f' to float values.
    StringBuilder sb = new StringBuilder();
    String names[] = getNames();
    for (int index = 0; index < names.length; index++) {
      sb.append(prefix + names[index] + " = ");
      PrimitiveArray pa = hashmap.get(names[index]);
      String connect = "";
      boolean isCharArray = pa instanceof CharArray;
      if (pa.elementType() == PAType.STRING || isCharArray) {
        int n = pa.size();
        for (int i = 0; i < n; i++) {
          sb.append(connect);
          connect = ", ";
          sb.append(String2.toJson(pa.getRawString(i), 65536, isCharArray)); // encodeNewline?
        }
      } else if (pa instanceof FloatArray fa) {
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
   * This makes destination's contents equal this Attribute's contents. The values (primitiveArrays)
   * are cloned.
   *
   * @param destination the Attributes which will be made equal to 'source'.
   */
  public void copyTo(Attributes destination) {
    destination.hashmap.clear();
    Enumeration en = hashmap.keys();
    while (en.hasMoreElements()) {
      String name = (String) en.nextElement();
      destination.set(name, (PrimitiveArray) get(name).clone());
    }
  }

  /**
   * This tests if o is an Attributes and has the same data. This doesn't throw an Exception if a
   * difference is found.
   *
   * @param o an object, presumably an Attributes
   */
  @Override
  public boolean equals(Object o) {
    return testEquals(o).length() == 0;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * This returns a string indicating the differents of this Attributes and o, or "" if no
   * difference. This doesn't throw an Exception if a difference is found.
   *
   * @param o an object, presumably an Attributes
   * @return a string indicating the differents of this Attributes and o, or "" if no difference.
   */
  public String testEquals(Object o) {
    if (o == null) return "The new Attributes object is null.";
    if (!(o instanceof Attributes)) return "The new object isn't an Attributes object.";
    return Test.testEqual(toString(), ((Attributes) o).toString(), "");
  }

  /**
   * This returns a netcdf-style String representation of a PrimitiveArray: StringArray is newline
   * separated, others are comma separated.
   *
   * @param pa any PrimitiveArray
   * @return a String representation of a value PrimitiveArray.
   */
  public static String valueToNcString(PrimitiveArray pa) {
    if (pa.elementType() == PAType.STRING)
      return String2.toSVString(
          ((StringArray) pa).toStringArray(), "\n", false); // false=no trailing newline
    return pa.toString();
  }

  /**
   * This goes through all of this Atts (addAtts?) and otherAtts (sourceAtts?) and adds a trim'd
   * version to this Atts if any att has whitespace at beginning or end.
   *
   * @param otherAtts another Attributes object
   */
  public void trimIfNeeded(Attributes otherAtts) {
    if (otherAtts == null) return;

    // go through all atts in this or otherAtts
    HashSet<String> set = new HashSet();
    set.addAll(otherAtts.hashmap.keySet());
    set.addAll(hashmap.keySet());
    Iterator<String> it = set.iterator();
    while (it.hasNext()) {
      String name = it.next();
      PrimitiveArray pa = get(name); // prefer from this Atts
      if (pa == null) pa = otherAtts.get(name);
      if (pa == null) continue;
      if (pa.elementType() == PAType.STRING) {
        boolean changed = false;
        int tSize = pa.size();
        StringArray newSa = new StringArray();
        for (int i = 0; i < tSize; i++) {
          String s = pa.getString(i);
          String ts = null;
          if (s != null) {
            ts = i == 0 ? s.trim() : String2.trimEnd(s);
            if (ts.length() < s.length()) changed = true;
          }
          newSa.add(ts);
        }
        if (changed) set(name, newSa);
      }
    }
  }

  /**
   * This removes keys and values from this Attributes which are exactly equal (even same data type)
   * in otherAtts.
   *
   * @param otherAtts another Attributes object
   */
  public void removeIfSame(Attributes otherAtts) {
    if (otherAtts == null) return;

    // go through this
    Iterator it =
        hashmap.keySet().iterator(); // iterator (not enumeration) since I use it.remove() below
    while (it.hasNext()) {
      String name = (String) it.next();
      PrimitiveArray otherPa = otherAtts.get(name);
      if (otherPa != null) {
        PrimitiveArray pa = get(name);
        if (pa.equals(otherPa))
          // if atts are equal, remove in this Attributes
          it.remove();
      }
    }
  }

  /** This trim()s all names/keys and trim()s all 1 String values. */
  public void trim() {
    Iterator it = hashmap.keySet().iterator();
    while (it.hasNext()) {
      String name = (String) it.next();
      String tName = name.trim();
      PrimitiveArray pa = null;
      if (name.equals(tName)) {
        pa = get(name);
      } else {
        // switch to trim'd name
        pa = remove(name);
        set(name = tName, pa);
      }

      // trim value?
      if (pa.elementType() == PAType.STRING && pa.size() > 0) {
        pa.setString(0, String2.trimStart(pa.getRawString(0)));
        pa.setString(pa.size() - 1, String2.trimEnd(pa.getRawString(pa.size() - 1)));
      }
    }
  }

  /**
   * This trim()s and makesValidUnicode all names/keys and all 1 String values. For multi-string
   * values, this makes them valid Unicode.
   */
  public void trimAndMakeValidUnicode() {
    Iterator it = hashmap.keySet().iterator();
    while (it.hasNext()) {
      String name = (String) it.next();
      String tName = String2.makeValidUnicode(name.trim(), "\r\n\t");

      PrimitiveArray pa = null;
      if (name.equals(tName)) {
        pa = get(name);
      } else {
        // switch to trim/valid name
        pa = remove(name);
        set(name = tName, pa);
      }

      // trim/makeValid the value?
      if (pa.elementType() == PAType.STRING && pa.size() > 0) {
        int tSize = pa.size();
        pa.setString(0, String2.trimStart(pa.getRawString(0)));
        pa.setString(tSize - 1, String2.trimEnd(pa.getRawString(tSize - 1)));
        for (int i = 0; i < tSize; i++) {
          String s = String2.makeValidUnicode(pa.getRawString(i), "\r\n\t");
          s =
              String2.commonUnicodeToPlainText(
                  s); // a little aggressive, but reasonable for attributes
          pa.setString(0, s);
        }
      }
    }
  }

  /** This uses StringArray.fromNccsv() on all StringArray values to de-JSON and convert "" to ". */
  public void fromNccsvStrings() {
    Iterator it = hashmap.keySet().iterator();
    while (it.hasNext()) {
      String name = (String) it.next();
      PrimitiveArray pa = get(name);
      if (pa.elementType() == PAType.STRING) ((StringArray) pa).fromNccsv();
    }
  }

  /**
   * This makes a set of addAttributes which are needed to change a into b. If an attribute in 'a'
   * needs to be set to null, this sets it to the String "null" instead of just nulling it.
   *
   * @param a an Attributes object
   * @param b another Attributes object
   * @return a set of Attributes which are needed to change a into b.
   */
  public static Attributes makeALikeB(Attributes a, Attributes b) {
    Attributes addAtts = new Attributes();

    // remove/change atts already in 'a' that aren't correct
    String[] aNames = a.getNames();
    int naNames = aNames.length;
    for (int i = 0; i < naNames; i++) {
      String aName = aNames[i];
      PrimitiveArray aPA = a.get(aName);
      PrimitiveArray bPA = b.get(aName);
      if (bPA == null) addAtts.set(aName, "null");
      else if (!aPA.equals(bPA)) addAtts.set(aName, bPA);
    }

    // add atts from 'b' that aren't already in 'a'
    String[] bNames = b.getNames();
    int nbNames = bNames.length;
    for (int i = 0; i < nbNames; i++) {
      String bName = bNames[i];
      if (a.get(bName) == null) addAtts.set(bName, b.get(bName));
    }

    return addAtts;
  }

  /**
   * This writes the attributes for a variable (or *GLOBAL*) to an NCCSV String. This doesn't write
   * *SCALAR* or dataType attributes. This doesn't change any of the attributes.
   *
   * @param varName The name of the variable to which these attributes are associated.
   * @return a string with all of the attributes for a variable (or *GLOBAL*) formatted for NCCSV.
   */
  public String toNccsvString(String varName) {
    String nccsvVarName = String2.toNccsvDataString(varName);
    StringBuilder sb = new StringBuilder();
    String tName;

    // special case: *GLOBAL* Conventions
    if (varName.equals(String2.NCCSV_GLOBAL)) {
      tName = "Conventions";
      String val = getString(tName);
      if (String2.isSomething(val)) {
        if (val.indexOf("NCCSV") >= 0)
          val =
              val.replaceAll(
                  "NCCSV-\\d\\.\\d",
                  String2
                      .NCCSV_VERSION); // string.replaceAll(regex, newS) should match. No change if
        // not.
        else val += ", " + String2.NCCSV_VERSION;
      } else {
        val = "COARDS, CF-1.10, ACDD-1.3, " + String2.NCCSV_VERSION;
      }
      sb.append(
          String2.toNccsvDataString(varName)
              + ","
              + String2.toNccsvDataString(tName)
              + ","
              + String2.toNccsvAttString(val)
              + "\n");
    }

    // each of the attributes
    String names[] = getNames();
    for (int ni = 0; ni < names.length; ni++) {
      tName = names[ni];
      if (varName.equals(String2.NCCSV_GLOBAL) && tName.equals("Conventions")) continue;
      if (!String2.isSomething(tName) || tName.equals("_NCProperties")) continue;
      PrimitiveArray tValue = get(tName);
      if (tValue == null || tValue.size() == 0 || tValue.toString().length() == 0)
        continue; // do nothing
      sb.append(
          String2.toNccsvDataString(nccsvVarName)
              + ","
              + String2.toNccsvDataString(tName)
              + ","
              + tValue.toNccsvAttString()
              + "\n");
    }
    return sb.toString();
  }

  /**
   * This throws a RuntimeException if any attribute name is !String2.isVariableNameSafe(attName).
   *
   * @param sourceDescripton e.g., "In the combined attributes for the variable with
   *     destinationName="sst"". This is just used in the error message.
   */
  public void ensureNamesAreVariableNameSafe(String sourceDescription) {
    String names[] = getNames();
    for (int ni = 0; ni < names.length; ni++) {
      if (!String2.isVariableNameSafe(names[ni]))
        throw new RuntimeException(
            sourceDescription
                + ", attributeName="
                + String2.toJson(names[ni])
                + " isn't variableNameSafe. It must start with iso8859Letter|_ and contain only iso8859Letter|_|0-9 .");
    }
  }

  /**
   * This writes the attributes for a variable (or *GLOBAL*) to an NCCSV Binary DataOutputStream.
   * This doesn't write *SCALAR* or dataType attributes. This doesn't change any of the attributes.
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
   * This writes the attributes for a variable (or *GLOBAL*) to a String using NCO JSON lvl=2
   * pedantic style. See https://nco.sourceforge.net/nco.html#json This doesn't change any of the
   * attributes. See issues in javadoc for EDDTable.saveAsNcoJson().
   *
   * <p>String attributes are written as type="char". See comments in EDDTable.
   *
   * @param indent a String a spaces for the start of each line
   * @return a string with all of the attributes for a variable (or *GLOBAL*) formatted for NCO
   *     JSON, with a comma after the closing }.
   */
  public String toNcoJsonString(String indent) {
    StringBuilder sb = new StringBuilder();

    // "attributes": {
    //  "byte_att": { "type": "byte", "data": [0, 1, 2, 127, -128, -127, -2, -1]},
    //  "char_att": { "type": "char", "data": "Sentence one.\nSentence two.\n"},
    //  "short_att": { "type": "short", "data": 37},
    //  "int_att": { "type": "int", "data": 73},
    //  "long_att": { "type": "int", "data": 73},                            //???!!! why not
    // "type"="long"???
    //  "float_att": { "type": "float", "data": [73.0, 72.0, 71.0, 70.010, 69.0010, 68.010,
    // 67.010]},
    //  "double_att": { "type": "double", "data": [73.0, 72.0, 71.0, 70.010, 69.0010, 68.010,
    // 67.0100010]}
    // },
    sb.append(indent + "\"attributes\": {\n");

    // each of the attributes
    String names[] = getNames();
    boolean somethingWritten = false;
    for (int ni = 0; ni < names.length; ni++) {
      String tName = names[ni];
      PrimitiveArray pa = get(tName);
      if (pa == null || pa.size() == 0) continue; // do nothing
      String tType = pa.elementTypeString();
      boolean isString = tType.equals("String");
      // In NCO JSON, all char and String attributes appear as 1 string with type=char.
      // There are no "string" attributes.
      if (isString) {
        tType = "char";
      } else if (tType.equals("long")) {
        tType = "int64"; // see
        // https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_data_types and NCO JSON examples
      } else if (tType.equals("ulong")) {
        tType = "uint64";
      }
      sb.append(
          (somethingWritten ? ",\n" : "")
              + indent
              + "  "
              + String2.toJson(tName)
              + ": {\"type\": \""
              + tType
              + "\", \"data\": ");
      if (isString) {
        String s = ((StringArray) pa).toNewlineString();
        sb.append(String2.toJson(s.substring(0, s.length() - 1))); // remove trailing \n
      } else if (pa.elementType() == PAType.CHAR) {
        // each char becomes a string, then displayed as newline separated string
        // if (tName.equals("_FillValue")) String2.pressEnterToContinue(">> ncoJson _FillValue=" +
        // String2.annotatedString((new StringArray(pa)).toString()) + " maxIsMV=" +
        // pa.getMaxIsMV());
        String s = new StringArray(pa).toNewlineString();
        sb.append(String2.toJson(s.substring(0, s.length() - 1))); // remove trailing \n
      } else {
        String js = pa.toJsonCsvString();
        sb.append(pa.size() == 1 ? js : "[" + js + "]");
      }
      sb.append('}');
      somethingWritten = true;
    }
    sb.append(
        (somethingWritten ? "\n" : "")
            + // end previous line
            indent
            + "},\n");
    return sb.toString();
  }

  /**
   * This is standardizes one data variable's atts and pa.
   *
   * @param standardizeWhat This determines what gets done. It is the sum of these options (when
   *     applicable): <br>
   *     1: apply scale_factor and add_offset, if _Unsigned=true, make signed PA into unsigned PA,
   *     convert defined numeric _FillValue's and missing_value's to "standard" values (MAX_VALUE
   *     for integer types, and NAN for doubles and floats), and unpack packed attributes (e.g.,
   *     actual|data|valid_min|max|range) (if packed). <br>
   *     2: convert defined numeric dateTimes to "seconds since 1970-01-01T00:00:00Z". (If this is
   *     selected and there is a chance the data has scale_factor or add_offset, #1 must be selected
   *     also.) <br>
   *     4: convert defined string _FillValue's and missing_value's to "". <br>
   *     256: if dataPa is numeric, if _FillValue and missing_value aren't defined, try to identify
   *     an undefined numeric missing_value (-999?, 9999?, 1e37f?) and convert instances to
   *     "standard" values: MAX_VALUE for integer types, and NAN for doubles and floats. This is
   *     done before #1 and #2. <br>
   *     512: if dataPa is Strings, convert all common, undefined, missing value strings (e.g., ".",
   *     "...", "-", "?", "???", "N/A", "none", "null", "unknown", "unspecified" (it is
   *     case-insensitive)) to "". "nd" and "other" are not on the list. See code for
   *     String2.isSomething2() for the current list. <br>
   *     1024: if dataPa is Strings, try to convert not-purely-numeric String dateTimes (e.g., "Jan
   *     2, 2018") to ISO 8601 String dateTimes. <br>
   *     2048: if dataPa is Strings, try to convert purely-numeric String dateTimes (e.g.,
   *     "20180102") to ISO 8601 String dateTimes. <br>
   *     4096: This tries to standardize the units string for each variable. For example, "meters
   *     per second", "meter/second", "m.s^-1", "m s-1", "m.s-1" will all be converted to "m.s-1".
   *     This doesn't change the data values. This works well for valid UDUNITS units strings, but
   *     can have problems with invalid or complex strings. You can deal with problems by specifying
   *     specific from-to pairs in &lt;standardizeUdunits&gt; in messages.xml. Please email any
   *     changes you make to erd.data at noaa.gov so they can be incorporated into the default
   *     messages.xml. <br>
   *     So a common, do-all-safe-things value is: 7 (1+2+4). <br>
   *     And if standardizeWhat == 0, this just returns dataPa.
   * @param varName is for diagnostic messages only
   * @param dataPa the (possibly packed) pa for the variable
   * @return the same dataPa or a new dataPa of a different type.
   * @throws Exception if trouble
   */
  public PrimitiveArray standardizeVariable(
      int standardizeWhat, String varName, PrimitiveArray dataPa) throws Exception {
    // I had alternatives to 1024 and 2048 that converted String times
    // to epochSeconds, but removed them because:
    // many users e.g., EDDTableFromAsciiFiles has already converted
    // the columns to the destination data type (e.g., double) before
    // doing the standardize (which would expect strings).
    // Rather than deal will complications about which options can be used
    // when, I just kept the options (1024 and 2048) that keep the dataType the same.

    if (standardizeWhat == 0) return dataPa;
    if ((standardizeWhat & 2) != 0 && (standardizeWhat & 1) == 0)
      throw new RuntimeException(
          String2.ERROR
              + " in Attributes.standardizeVariable: if standardizeWhat("
              + standardizeWhat
              + ") includes 2, it must also include 1.");
    if (debugMode) String2.log(">> standardizeVariable varName=" + varName);
    Attributes newAtts = this; // so it has a more descriptive name
    Attributes oldAtts = new Attributes(this); // so we have an unchanged copy to refer to
    String oUnits = oldAtts.getString("units");

    // determine what the destClass will be (for packed numeric variables)
    PAType oPAType = dataPa.elementType();
    boolean isNumeric = oPAType != PAType.CHAR && oPAType != PAType.STRING;
    boolean isString = oPAType == PAType.STRING;
    PAType destPAType = oPAType;

    // determine if this is numeric dateTimes that will be converted to epochSeconds
    boolean standardizeNumericDateTimes =
        (standardizeWhat & 2) == 2 && isNumeric && Calendar2.isNumericTimeUnits(oUnits);

    // if standardizeWhat & 256, if _FillValue and missing_value aren't defined,
    //    try to identify an undefined numeric missing_value (-999?, 9999?, 1e37f?)
    //    and convert them to "standard" values: MAX_VALUE for integer types, and NAN for floats.
    // Do this BEFORE #1 and #2
    if ((standardizeWhat & 256) == 256 && isNumeric) {
      PrimitiveArray mvPA = oldAtts.get("missing_value");
      PrimitiveArray fvPA = oldAtts.get("_FillValue");
      if (mvPA == null && fvPA == null) {
        // look for missing_value = -99, -999, -9999, -99999, -999999, -9999999
        PAOne mv = dataPa.tryToFindNumericMissingValue();
        if (mv != null) {
          dataPa.switchFromTo("" + mv, "");
          if (debugMode) String2.log(">> #256: switched mv=" + mv + " to \"standard\" mv.");
        }
      }
    }

    // if standardizeWhat & 1, apply _Unsigned, scale_factor, and add_offset; change missing_value,
    // _FillValue
    if ((standardizeWhat & 1) == 1 && isNumeric) {
      PrimitiveArray unsignedPA = newAtts.remove("_Unsigned");
      PrimitiveArray scalePA = newAtts.remove("scale_factor");
      PrimitiveArray addPA = newAtts.remove("add_offset");
      boolean unsigned = unsignedPA != null && "true".equals(unsignedPA.toString());
      double scale = 1;
      double add = 0;
      if (scalePA != null) {
        scale = scalePA.getNiceDouble(0);
        if (Double.isNaN(scale)) scale = 1;
      }
      if (addPA != null) {
        add = addPA.getNiceDouble(0);
        if (Double.isNaN(add)) add = 0;
      }

      // get _FillValue and missing_value
      // If dataPa is unsigned, these are already unsigned
      // If dataPa is packed, these are packed too.
      double dFillValue =
          unsigned
              ? oldAtts.getUnsignedDouble("_FillValue")
              : // so -1b becomes 255, works whether source is unsigned or not
              oldAtts.getDouble("_FillValue");
      // String2.log(">> oldAtts=\n" + oldAtts.toString() + ">> scalePA=" + scalePA + " addPA=" +
      // addPA + " dFillValue=" + dFillValue);
      double dMissingValue =
          unsigned
              ? oldAtts.getUnsignedDouble("missing_value")
              : // so -1b becomes 255, works whether source is unsigned or not
              oldAtts.getDouble("missing_value");
      newAtts.remove("missing_value");
      newAtts.remove("_FillValue");
      double dataPaMV = dataPa.missingValue().getRawDouble();

      if (unsigned || scalePA != null || addPA != null) {
        if (debugMode && unsigned) String2.log(">> #1: unsigned=true");

        // make changes
        destPAType =
            standardizeNumericDateTimes
                ? PAType.DOUBLE
                : scalePA != null
                    ? scalePA.elementType()
                    : addPA != null
                        ? addPA.elementType()
                        : unsigned && oPAType == PAType.BYTE
                            ? PAType.UBYTE
                            : // similar code below   //trouble (not), but change to not doing
                            // this???
                            unsigned && oPAType == PAType.SHORT
                                ? PAType.USHORT
                                : unsigned && oPAType == PAType.INT
                                    ? PAType.UINT
                                    : unsigned && oPAType == PAType.LONG
                                        ? PAType.ULONG
                                        : destPAType;

        // switch data type
        if (debugMode)
          String2.log(
              ">> switch data type: unsigned="
                  + unsigned
                  + " oPAType="
                  + oPAType
                  + " dMV="
                  + dMissingValue
                  + " dFV="
                  + dFillValue
                  + " destPAType="
                  + destPAType
                  + " dataPA type="
                  + dataPa.elementType()
                  + " dataPaMV="
                  + dataPaMV
                  + " dataPa= "
                  + dataPa.toString());
        dataPa =
            // if stated missing_value or _FillValue is same as cohort missingValue...
            unsigned
                ? dataPa.makeUnsignedPA()
                : // unsigned
                dataPa.isIntegerType()
                        && (dMissingValue == dataPaMV
                            || // e.g., 127 == 127
                            dFillValue == dataPaMV)
                    ? PrimitiveArray.factory(destPAType, dataPa.setMaxIsMV(true))
                    : // missingValues (e.g., 127) are    changed, e.g., to NaN
                    PrimitiveArray.rawFactory(
                        destPAType, dataPa); // missingValues (e.g., 127) AREN'T changed
        // so unsigned values are now properly unsigned
        // String2.log(">> switch data type2: dataPa= " + dataPa.toString());

        // If present, missing_value and _FillValue are packed
        // so convert other dMissingValue and dFillValue (e.g., -128) to PA standard mv
        // after changing class (so unsigned is applied) but before applying scaleAddOffset
        // String2.log("> dFillValue=" + dFillValue + " dataPa.missingValue=" +
        // dataPa.missingValue());
        if (!Double.isNaN(dMissingValue) && dMissingValue != dataPaMV)
          dataPa.switchFromTo("" + dMissingValue, "");
        if (!Double.isNaN(dFillValue) && dFillValue != dataPaMV)
          dataPa.switchFromTo("" + dFillValue, "");

        // apply scaleAddOffset
        if (scale != 1 || add != 0) {
          dataPa.scaleAddOffset(
              scale, add); // it checks for (1,0).  CoHort missing values are unaffected.
          if (debugMode) String2.log(">> #1: scale_factor=" + scale + " add_offset=" + add);
        }

        // standardize other packed atts
        // Atts are always already unsigned.
        // Whether these are initially packed or standardizeed is not reliable.
        //  So if these are already float or doubles, assume already standardizeed
        String names[] = {
          "actual_min", "actual_max", "actual_range",
          "data_min", "data_max", "data_range",
          "valid_min", "valid_max", "valid_range"
        };
        if (destPAType == PAType.FLOAT || destPAType == PAType.DOUBLE) {
          for (int i = 0; i < names.length; i++) {
            PrimitiveArray pa = newAtts.get(names[i]);
            if (pa != null
                && pa.elementType() != PAType.FLOAT
                && pa.elementType() != PAType.DOUBLE) {
              pa = PrimitiveArray.factory(destPAType, pa);
              pa.scaleAddOffset(scale, add);
              newAtts.set(names[i], pa);
            }
          }
        } else if (unsigned) {
          for (int i = 0; i < names.length; i++) {
            PrimitiveArray pa = newAtts.get(names[i]);
            if (pa != null && pa.isIntegerType() && !pa.isUnsigned()) {
              newAtts.set(names[i], pa.makeUnsignedPA());
            }
          }
        }

      } else { // if not unsigned or packed, still need to convert missing values

        dataPa.convertToStandardMissingValues(
            oldAtts.getString("_FillValue"), oldAtts.getString("missing_value"));
      }

      // For all types other than String, need to specify _FillValue=standard missing value
      if (destPAType != PAType.STRING)
        newAtts.add("_FillValue", PrimitiveArray.factory(destPAType, 1, ""));
    }

    // if standardizeWhat & 2, convert numeric dateTimes to "seconds since 1970-01-01T00:00:00Z".
    if (standardizeNumericDateTimes) {
      // If #2 selected, #1 (unsigned, scale_factor, add_offset, missing_value, _FillValue)
      // was automatically done above.

      // interpret tUnits
      double[] baseFactor = Calendar2.getTimeBaseAndFactor(oUnits); // throws exception

      // convert numeric time to epochSeconds (in DoubleArray)
      dataPa = Calendar2.unitsSinceToEpochSeconds(baseFactor[0], baseFactor[1], dataPa);
      newAtts.set("units", Calendar2.SECONDS_SINCE_1970);

      // convert mv, fv, and others to double and scale/addOffset
      String names[] = {
        "actual_min", "actual_max", "actual_range",
        "data_min", "data_max", "data_range",
        "valid_min", "valid_max", "valid_range",
        "_FillValue", "missing_value"
      };
      for (int i = 0; i < names.length; i++) {
        PrimitiveArray pa = newAtts.remove(names[i]);
        if (pa != null) {
          pa = new DoubleArray(pa);
          pa.scaleAddOffset(baseFactor[1], baseFactor[0]);
          newAtts.add(names[i], pa);
        }
      }

      if (debugMode)
        String2.log(
            ">> #2: converted numeric dateTimes ("
                + oUnits
                + ") to epochSeconds \"seconds since 1970-01-01T00:00:00Z\".");
    }

    // if standardizeWhat & 4, convert defined String _FillValue's and missing_value's to standard
    // cohort mv
    if ((standardizeWhat & 4) == 4 && dataPa.elementType() == PAType.STRING) {
      newAtts.remove("missing_value");
      newAtts.remove("_FillValue");
      dataPa.convertToStandardMissingValues(
          oldAtts.getString("_FillValue"), oldAtts.getString("missing_value"));
      if (debugMode)
        String2.log(">> #4: converted defined String _FillValue's and missing_value's to \"\".");
    }

    // if standardizeWhat & 512, convert all common, undefined, missing value strings (e.g., "-",
    // "N/A", "NONE", "?") and convert them to "".
    if ((standardizeWhat & 512) == 512 && dataPa.elementType() == PAType.STRING) {
      int n = dataPa.size();
      String nothing = String2.canonical("");
      for (int i = 0; i < n; i++) {
        if (!String2.isSomething2(dataPa.getRawString(i))) dataPa.setString(i, nothing);
      }
      if (debugMode) String2.log(">> #512: converted undefined missing value strings to \"\".");
    }

    // string dateTimes
    if ((standardizeWhat & (1024 + 2048)) != 0
        && (dataPa.isIntegerType() || dataPa.elementType() == PAType.STRING)) {

      int n = dataPa.size();
      String firstS = null;
      for (int i = 0; i < n; i++) {
        if (String2.isSomething(dataPa.getString(i))) {
          firstS = dataPa.getString(i);
          break;
        }
      }

      boolean allDigits = false;
      String format = "";
      if (firstS == null) {
      } else {
        allDigits = String2.allDigits(firstS);
        format = Calendar2.suggestDateTimeFormat(dataPa, true); // evenIfPurelyNumeric
        // String2.log(">> standardize 2048 firstS=" + firstS + " format=" + format);
        // format will be "" if no match
      }

      // if standardizeWhat & 1024, try to convert not-purely-numeric String dateTimes
      //  (e.g., "Jan 2, 2018") to ISO 8601 String dateTimes.
      if ((standardizeWhat & 1024) == 1024 && format.length() > 0 && !allDigits) {

        DoubleArray da = Calendar2.parseToEpochSeconds(dataPa, format); // NaN's if trouble
        if (dataPa.elementType() == PAType.STRING) dataPa.clear();
        else dataPa = new StringArray();

        // if source format has time, keep time in iso 8601 format (even if 00:00:00)
        if (format.indexOf('S') >= 0) {
          // if source format has millis, keep time.millis in iso format (even if 00:00:00.000)
          for (int i = 0; i < n; i++)
            dataPa.addString(Calendar2.safeEpochSecondsToIsoStringT3Z(da.get(i), ""));
          newAtts.set("units", Calendar2.ISO8601T3Z_FORMAT);
        } else if (format.indexOf('H') >= 0) {
          // if source format has time, keep time in iso 8601 format (even if 00:00:00)
          for (int i = 0; i < n; i++)
            dataPa.addString(Calendar2.safeEpochSecondsToIsoStringTZ(da.get(i), ""));
          newAtts.set("units", Calendar2.ISO8601TZ_FORMAT);
        } else {
          // else just date
          for (int i = 0; i < n; i++)
            dataPa.addString(Calendar2.safeEpochSecondsToIsoDateString(da.get(i), ""));
          newAtts.set("units", Calendar2.ISO8601DATE_FORMAT);
        }

        if (debugMode)
          String2.log(
              ">> #1024: converted not-purely-numeric String dateTimes (e.g., \""
                  + firstS
                  + "\") to ISO 8601 String dateTimes.");
      }

      // if standardizeWhat & 2048, try to convert purely-numeric String or integer dateTimes
      //  (e.g., "20180102") to ISO 8601 String dateTimes.
      if ((standardizeWhat & 2048) == 2048 && format.length() > 0 && allDigits) {

        DoubleArray da = Calendar2.parseToEpochSeconds(dataPa, format); // NaN's if trouble
        if (dataPa.elementType() == PAType.STRING) dataPa.clear();
        else dataPa = new StringArray();

        if (format.indexOf('S') >= 0) {
          // if source format has millis, keep time.millis in iso 8601 format (even if 00:00:00.000)
          for (int i = 0; i < n; i++)
            dataPa.addString(Calendar2.safeEpochSecondsToIsoStringT3Z(da.get(i), ""));
          newAtts.set("units", Calendar2.ISO8601T3Z_FORMAT);
        } else if (format.indexOf('H') >= 0) {
          // if source format has time, keep time in iso 8601 format (even if 00:00:00)
          for (int i = 0; i < n; i++)
            dataPa.addString(Calendar2.safeEpochSecondsToIsoStringTZ(da.get(i), ""));
          newAtts.set("units", Calendar2.ISO8601TZ_FORMAT);
        } else {
          // else just date
          for (int i = 0; i < n; i++)
            dataPa.addString(Calendar2.safeEpochSecondsToIsoDateString(da.get(i), ""));
          newAtts.set("units", Calendar2.ISO8601DATE_FORMAT);
        }

        if (debugMode)
          String2.log(
              ">> #2048, converted purely-numeric String dateTimes (e.g., "
                  + firstS
                  + ") to ISO 8601 String dateTimes.");
      }
    }

    // if standardizeWhat & 4096, try to standardize the units.
    if ((standardizeWhat & 4096) == 4096) {

      String tUnits = newAtts.getString("units");
      if (tUnits != null) newAtts.set("units", Units2.safeStandardizeUdunits(tUnits));

      if (debugMode) String2.log(">> #4096, standardized units.");
    }

    return dataPa;
  }

  /**
   * Given these sourceAtts (which won't be changed), this unpacks the dataPa. If the var has time
   * units, or scale_factor and add_offset, the values in the dataPa will be unpacked (numeric times
   * will be epochSeconds). If missing_value and/or _FillValue are used, they are converted to PA
   * standard mv.
   *
   * @param varName the var's fullName, for diagnostic messages only
   * @param dataPa This may be an attribute's pa or a variable's dataPa. If null, no error and
   *     nothing is done.
   * @param dataPa This may be a dataVariable's pa, or one attribute's pa (e.g., valid_min).
   * @param lookForStringTimes If true, this tries to convert string times (but not purely numeric
   *     formats because too many false positives).
   * @param lookForUnsigned If true, this looks for _Unsigned=true and unpacks dataPa. If dataPA is
   *     an attribute's pa, this is usually false.
   * @return The same dataPa or a new dataPa or null (if the dataPa parameter = null). missing_value
   *     and _FillValue will be converted to PA standard mv for the the unpacked datatype (or
   *     current type if not packed).
   */
  public PrimitiveArray unpackPA(
      String varName, PrimitiveArray dataPa, boolean lookForStringTimes, boolean lookForUnsigned) {

    if (dataPa == null) return null;
    Attributes sourceAtts = this; // so it has a nice name
    if (debugMode) String2.log(">> Attributes.unpackPA(var=" + varName + ")");

    // handle _Unsigned first
    boolean unsigned = lookForUnsigned && "true".equals(sourceAtts.getString("_Unsigned"));
    if (unsigned) dataPa = dataPa.makeUnsignedPA();

    // any of these may be null
    PAType dataPaPAType = dataPa.elementType();
    PrimitiveArray scalePA = sourceAtts.get("scale_factor");
    PrimitiveArray addPA = sourceAtts.get("add_offset");
    String tUnits = sourceAtts.getString("units");
    double dataPaMV = dataPa.missingValue().getRawDouble();
    // _FillValue and missing_value should be unsigned if _Unsigned=true,
    //  but in aren't in .nc3 files
    // and they should be packed if var is packed.
    // see EDDGridFromNcFilesUnpacked.testUInt16File()
    double dFillValue =
        unsigned
            ? sourceAtts.getUnsignedDouble("_FillValue")
            : // so -1b becomes 255
            sourceAtts.getDouble("_FillValue");
    double dMissingValue =
        unsigned
            ? sourceAtts.getUnsignedDouble("missing_value")
            : // so -1b becomes 255
            sourceAtts.getDouble("missing_value");
    if (debugMode) String2.log(">>   _FillValue=" + dFillValue + " missing_value=" + dMissingValue);

    // scale and add_offset -- done first, before check for numeric time
    if (scalePA != null || addPA != null) {

      // figure out new data type indicated by scale_factor or add_offset
      double scale = 1;
      double add = 0;
      PAType tPAType = null; // null is important, tests if set below
      if (scalePA != null) {
        scale = scalePA.getNiceDouble(0);
        if (Double.isNaN(scale)) scale = 1;
        tPAType = scalePA.elementType();
      }
      if (addPA != null) {
        add = addPA.getNiceDouble(0);
        if (Double.isNaN(add)) add = 0;
        if (tPAType == null) tPAType = addPA.elementType();
      }
      if (tPAType == null) // might be
      tPAType = dataPaPAType;
      boolean willScale = scale != 1 || add != 0;

      // switch data type
      if (dataPa.isIntegerType() && dMissingValue == dataPaMV) { // e.g., 127 == 127
        dataPa.setMaxIsMV(true);
        dMissingValue = Double.NaN; // it's done
      }
      if (dataPa.isIntegerType() && dFillValue == dataPaMV) { // e.g., 127 == 127
        dataPa.setMaxIsMV(true);
        dFillValue = Double.NaN; // it's done
      }
      PrimitiveArray dataPa2 =
          PrimitiveArray.factory(tPAType, dataPa); // if missing_value is MAX_VALUE, they become NaN
      if (debugMode)
        String2.log(
            ">>   source ="
                + dataPaPAType
                + ": "
                + dataPa.subset(0, 1, Math.min(10, dataPa.size() - 1)).toString()
                + "\n"
                + ">>   dataPa2="
                + dataPa2.elementType()
                + ": "
                + dataPa2.subset(0, 1, Math.min(10, dataPa2.size() - 1)).toString());

      // dataPa2 is destination data type and now has (if relevant) unsigned values,
      // but not yet scaleAddOffset.
      // If present, missing_value and _FillValue are packed
      // so convert other dMissingValue and dFillValue (e.g., -128) to PA standard mv
      // and apply before scaleAddOffset
      // String2.log(">>   dFillValue=" + dFillValue + " dataPa.missingValue=" +
      // dataPa.missingValue());
      if (!Double.isNaN(dMissingValue)) {
        if (debugMode) String2.log(">>     switchFromTo " + dMissingValue);
        dataPa2.switchFromTo("" + dMissingValue, "");
        dMissingValue = Double.NaN; // it's done
      }
      if (!Double.isNaN(dFillValue)) {
        if (debugMode) String2.log(">>     switchFromTo " + dFillValue);
        dataPa2.switchFromTo("" + dFillValue, "");
        dFillValue = Double.NaN; // it's done
      }

      // apply scaleAddOffset
      dataPa2.scaleAddOffset(scale, add); // it checks for (1,0)
      if (debugMode)
        String2.log(
            ">>   Attributes.unpackPA applied scale_factor="
                + scale
                + " add_offset="
                + add
                + "\n"
                + ">>   unpacked="
                + dataPa2.elementType()
                + ": "
                + dataPa2.subset(0, 1, Math.min(10, dataPa2.size() - 1)).toString());
      return dataPa2;
    }

    // numeric times (we know scale_factor and add_offset aren't used)
    if (Calendar2.isNumericTimeUnits(tUnits)) {

      // interpret tUnits
      double[] baseFactor = null;
      try {
        baseFactor = Calendar2.getTimeBaseAndFactor(tUnits); // throws exception
      } catch (Exception e) {
        String2.log(tUnits.toString());
        return PrimitiveArray.factory(PAType.DOUBLE, dataPa.size(), ""); // i.e. uninterpretable
      }

      // switch data type
      PrimitiveArray dataPa2 =
          // if stated missing_value or _FillValue is same as cohort missingValue...
          unsigned
              ? dataPa.makeUnsignedPA()
              : dataPa.isIntegerType()
                      && (dMissingValue == dataPaMV
                          || // e.g., 127 == 127
                          dFillValue == dataPaMV)
                  ? PrimitiveArray.factory(PAType.DOUBLE, dataPa)
                  : // missingValues (e.g., 127) are    changed
                  PrimitiveArray.rawFactory(
                      PAType.DOUBLE, dataPa); // missingValues (e.g., 127) AREN'T changed

      // convert other dMissingValue and dFillValue (e.g., -128)
      // before scaleAddOffset to epochSeconds
      if (!Double.isNaN(dMissingValue) && dMissingValue != dataPaMV) {
        dataPa2.switchFromTo("" + dMissingValue, "");
        dMissingValue = Double.NaN; // it's done
      }
      if (!Double.isNaN(dFillValue) && dFillValue != dataPaMV) {
        dataPa2.switchFromTo("" + dFillValue, "");
        dFillValue = Double.NaN; // it's done
      }

      // convert numeric time to epochSeconds
      dataPa2 = Calendar2.unitsSinceToEpochSeconds(baseFactor[0], baseFactor[1], dataPa2);
      if (debugMode)
        String2.log(
            ">>   numeric time as epochSeconds: "
                + dataPa2.subset(0, 1, Math.min(10, dataPa2.size() - 1)).toString());
      return dataPa2;
    }

    // string times?  no units or not reliable units, so units aren't helpful.
    if (lookForStringTimes && dataPa.elementType() == PAType.STRING) {
      StringArray sa = (StringArray) dataPa;
      String format = Calendar2.suggestDateTimeFormat(sa, false); // false for evenIfPurelyNumeric
      if (format.length() > 0) {
        if (verbose) String2.log("  " + varName + " has String times format=" + format);
        dataPa = Calendar2.parseToEpochSeconds(sa, format);
      }
      return dataPa;
    }

    // none of the above situations apply
    //  (i.e., not unsigned, and no scale_factor or add_offset, not numeric or string time)
    // still need to convert missingValue to dataPa missing value
    if (!Double.isNaN(dMissingValue) && dMissingValue != dataPaMV) {
      dataPa.switchFromTo("" + dMissingValue, "");
      dMissingValue = Double.NaN; // it's done
    }
    if (!Double.isNaN(dFillValue) && dFillValue != dataPaMV) {
      dataPa.switchFromTo("" + dFillValue, "");
      dFillValue = Double.NaN; // it's done
    }

    return dataPa;
  }

  /**
   * If _Unsigned=true, change tSourceType. This does not call
   * tSourceAttributes.convertSomeSignedToUnsigned().
   *
   * @param tSourceType the CoHort String name for the type. If !something, this returns current
   *     value. If invalid type, this throws exception.
   * @param tSourceAtts the source attributes.
   * @param tAddAtts the add attributes. If _Unsigned existed, _Unsigned=null will be added here.
   * @return the original tSourceType or the adjusted tSourceType.
   * @throws Exception if tSourceAtts or tAddAtts is null
   */
  public static String adjustSourceType(
      String tSourceType, Attributes tSourceAtts, Attributes tAddAtts) {
    if (!String2.isSomething(tSourceType)) return tSourceType;
    PAType paType = PAType.fromCohortStringCaseInsensitive(tSourceType); // throws exception

    PrimitiveArray us = tAddAtts.remove("_Unsigned");
    if (us == null) us = tSourceAtts.remove("_Unsigned");
    if (us == null) return tSourceType;

    if ("true".equals(us.toString())) paType = PAType.makeUnsigned(paType);
    else if ("false".equals(us.toString())) paType = PAType.makeSigned(paType);

    tAddAtts.set("_Unsigned", "null");

    return PAType.toCohortString(paType);
  }

  /**
   * This variant of adjustSourceType works with a pa, not sourceType string.
   *
   * @param pa the PrimitiveArray of source values. If null, this throws exception.
   * @param tSourceAtts the source attributes
   * @param tAddAtts the add attributes. If _Unsigned existed, _Unsigned=null will be added here.
   * @return the original tSourceType or the adjusted tSourceType.
   */
  public static PrimitiveArray adjustSourceType(
      PrimitiveArray pa, Attributes tSourceAtts, Attributes tAddAtts) {

    String tSourceType = adjustSourceType(pa.elementTypeString(), tSourceAtts, tAddAtts);
    return PrimitiveArray.factory(PAType.fromCohortStringCaseInsensitive(tSourceType), pa);
  }

  /**
   * Use this when a unsigned variable has been stored in a signed nc3 variable, because nc3 doesn't
   * support unsigned attributes. This converts some signed attributes into the correct unsigned
   * attributes.
   */
  public void convertSomeSignedToUnsigned() {
    // if var isUnsigned and select atts in nc3 file are signed, change to unsigned
    for (int i = 0; i < signedToUnsignedAttNames.length; i++) {
      String name = signedToUnsignedAttNames[i];
      PrimitiveArray tPa = get(name);
      if (tPa != null && !tPa.isUnsigned()) set(name, tPa.makeUnsignedPA());
    }
  }

  /**
   * Use this when a unsigned variable needs to be stored in a signed nc3 variable, because nc3
   * doesn't support unsigned attributes. This converts some unsigned attributes into temporary
   * signed attributes.
   */
  public void convertSomeUnsignedToSigned() {
    // if var isUnsigned and select atts in nc3 file are signed, change to unsigned
    for (int i = 0; i < signedToUnsignedAttNames.length; i++) {
      String name = signedToUnsignedAttNames[i];
      PrimitiveArray tPa = get(name);
      if (tPa != null && tPa.isUnsigned()) set(name, tPa.makeSignedPA());
    }
  }
}
