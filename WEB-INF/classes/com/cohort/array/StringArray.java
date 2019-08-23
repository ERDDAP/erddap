/* Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

/**
 * StringArray is a thin shell over a String[] with methods like ArrayList's 
 * methods; it extends PrimitiveArray.
 * All of the methods which add strings to StringArray (e.g., add()),
 * use String2.canonicalStringHolder(), to ensure that canonical Strings are stored (to save memory
 * if there are duplicates).
 *
 * <p>This class uses "" to represent a missing value (NaN).
 *
 * <p>Technically, this class might support element=null, but not fully tested.
 */
public class StringArray extends PrimitiveArray {

    static StringHolderComparator           stringHolderComparator           = new StringHolderComparator();
    static StringHolderComparatorIgnoreCase stringHolderComparatorIgnoreCase = new StringHolderComparatorIgnoreCase();

    /**
     * This is the main data structure.
     * This is private, because the Strings are stored as utf8 byte[].
     * Note that if the PrimitiveArray's capacity is increased,
     * the PrimitiveArray will use a different array for storage.
     * Active elements won't be null, 
     *   but string in an element may be null (but that's not fully supported/tested).
     */
    private StringHolder[] array; 
    
    /**
     * A constructor for a capacity of 8 elements. The initial 'size' will be 0.
     */
    public StringArray() {
        array = new StringHolder[8];
    }

    /**
     * This constructs a StringArray by copying elements from the incoming
     * PrimitiveArray (using append()).
     *
     * @param primitiveArray a primitiveArray of any type 
     */
    public StringArray(PrimitiveArray primitiveArray) {
        array = new StringHolder[primitiveArray.size()]; //exact size
        append(primitiveArray);
    }

    /**
     * A constructor for a specified number of elements. The initial 'size' will be 0.
     *
     * @param capacity creates an StringArray with the specified initial capacity.
     * @param active if true, size will be set to capacity and all elements 
     *    will equal "", else size = 0.
     */
    public StringArray(int capacity, boolean active) {
        Math2.ensureMemoryAvailable(16L * capacity, "StringArray"); //16 is lame estimate of space needed per String
        array = new StringHolder[capacity];
        if (active) {
            size = capacity;
            for (int i = 0; i < size; i++)
                array[i] = String2.STRING_HOLDER_ZERO;
        }
    }

    /**
     * A constructor which gets values from anArray[i].
     * THERE IS NO StringArray CONSTRUCTOR WHICH LET'S YOU SPECIFY THE BACKING ARRAY!
     * The values anArray are stored in a different way in a different data structure.
     *
     * @param anArray 
     */
    public StringArray(String[] anArray) {
        int al = anArray.length;
        array = new StringHolder[al];
        size = 0;
        for (int i = 0; i < al; i++) 
            add(anArray[i]);
    }

    /**
     * A constructor which gets values from anArray[i].toString().
     *
     * @param anArray 
     */
    public StringArray(Object[] anArray) {
        int al = anArray.length;
        array = new StringHolder[al];
        size = 0;
        for (int i = 0; i < al; i++) 
            add(anArray[i] == null? String2.EMPTY_STRING : anArray[i].toString());            
    }

    /**
     * A constructor which gets the toString values from the objects from an iterator.
     *
     * @param iterator  which needs to be thread-safe if the backing data store may be
     *    changed by another thread (e.g., use ConcurrentHashMap instead of HashMap).
     */
    public StringArray(Iterator iterator) {
        array = new StringHolder[8];
        while (iterator.hasNext()) {
            add(iterator.next().toString());
        }
    }

    /**
     * A constructor which gets the toString values from the objects from an enumeration.
     *
     * @param enumeration  which needs to be thread-safe if the backing data store may be
     *    changed by another thread (e.g., use ConcurrentHashMap instead of HashMap).
     */
    public StringArray(Enumeration enumeration) {
        array = new StringHolder[8];
        while (enumeration.hasMoreElements()) {
            add(enumeration.nextElement().toString());
        }
    }

    /**
     * A special method which encodes all the Unicode strings in this to ISO_8859_1.
     * Special chars are converted to "?".
     *
     * @return this for convenience
     */
    public StringArray toIso88591() {
        for (int i = 0; i < size; i++)
            set(i, String2.toIso88591String(get(i)));
        return this;
    }


    /**
     * A little weird: A special method which decodes all the strings with
     * UTF-8 bytes to Unicode.
     * See toUTF8().
     *
     * @return this for convenience
     */
    public StringArray fromUTF8() {
        for (int i = 0; i < size; i++)
            set(i, String2.utf8StringToString(get(i)));
        //String2.log(">>after fromUTF8: " + toNccsvAttString());
        return this;
    }

    /**
     * A little weird: A special method which encodes all the Unicode strings in 
     * this to UTF-8 bytes (stored as strings).
     * See fromUTF8().
     *
     * @return this for convenience
     */
    public StringArray toUTF8() {
        for (int i = 0; i < size; i++)
            set(i, String2.stringToUtf8String(get(i)));
        //String2.log(">>after toUTF8: " + toNccsvAttString());
        return this;
    }

    /**
     * A special method which encodes all the Unicode strings in this toJson(,127) encoding.
     *
     * @return this for convenience
     */
    public StringArray toJson() {
        for (int i = 0; i < size; i++)
            set(i, String2.toJson(get(i), 127));
        return this;
    }

    /** 
     * This converts a StringArray with JSON-encoded Strings into 
     * the actual (canonical) Strings.
     * This doesn't require that the JSON strings have enclosing double quotes.
     */
    public void fromJson() {
        for (int i = 0; i < size; i++) 
            set(i, String2.fromJsonNotNull(get(i))); //doesn't require enclosing "'s
    }

    /** 
     * This converts a StringArray with NCCSV-encoded Strings into 
     * the actual (canonical) Strings.
     * This doesn't require that the NCCSV strings have enclosing double quotes.
     */
    public void fromNccsv() {
        for (int i = 0; i < size; i++) 
            set(i, String2.fromNccsvString(get(i))); //doesn't require enclosing "'s
    }

    /* *  probably works, but not tested
     * This makes a StringArray with the contents of a map.
     * Each entry will be from <key>.toString() = <value>.toString().
     *
     * @param map  if it needs to be thread-safe, use ConcurrentHashMap
     * @return the corresponding String, with one entry on each line 
     *    (<key>.toString() = <value>.toString()) unsorted.
     *    Use sort() or sortIgnoreCase() afterwards if desired.
     * /    
    public StringArray(Map map) {
        Set keySet = map.keySet();
        array = new StringHolder[keySet.size()];
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            Object key = it.next();
            Object value = map.get(key);
            add(key.toString() + " = " + 
                (value == null? "null" : value.toString());
        }
    } */

    /**
     * This reads the text contents of the specified file using this computer's default charset.
     * 
     */
    public static StringArray fromFile(String fileName) throws Exception {
   
        return fromFile(fileName, null);
    }


    /**
     * This reads the text contents of the specified file and makes a StringArray
     * with an item from each line of the file (not trim'd).
     * 
     * @param charset e.g., ISO-8859-1 (the default); or "" or null for the default
     * @return StringArray with not canonical strings (on the assumption that 
     *   lines of all file are usually all different).
     * @throws Exception if trouble (e.g., file not found)
     */
    public static StringArray fromFile(String fileName, String charset) throws Exception {
        Math2.ensureMemoryAvailable(File2.length(fileName), "StringArray.fromFile"); //canonical may lessen memory requirement
        StringArray sa = new StringArray();
        BufferedReader bufferedReader = File2.getDecompressedBufferedFileReader(fileName, charset);
        try {
            String s = bufferedReader.readLine();
            while (s != null) { //null = end-of-file
                sa.addNotCanonical(s);
                s = bufferedReader.readLine();
            }
        } finally {
            try {bufferedReader.close();} catch (Exception e) {}
        }
        return sa;
    }

    /**
     * Like the other toFile, but uses the default charset and lineSeparator.
     */
    public void toFile(String fileName) throws Exception {
        toFile(fileName, null, null);
    }

    /**
     * This writes the strings to a file.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param charset e.g., UTF-8; or null or "" for the default (ISO-8859-1)
     * @param lineSeparator is the desired lineSeparator for the outgoing file.
     *     e.g., "\n".
     *     null or "" uses String2.lineSeparator (the standard separator for this OS).
     * @throws Exception if trouble (e.g., file can't be created).
     *    If trouble, this will delete any partial file.
     */
    public void toFile(String fileName, String charset, String lineSeparator) 
        throws Exception {
        
        if (lineSeparator == null || lineSeparator.length() == 0)
            lineSeparator = String2.lineSeparator;
        boolean append = false;
        Exception e = null;

        //bufferedWriter is declared outside try/catch so it
        //can be accessed from within either try/catch block.
        BufferedWriter bufferedWriter = null;
        try {
            //open the file
            if (charset == null || charset.length() == 0)
                charset = String2.ISO_8859_1;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(fileName, append)), charset));
                         
            //write the text to the file
            for (int i = 0; i < size; i++) {
                bufferedWriter.write(get(i));
                bufferedWriter.write(lineSeparator);
            }

        } catch (Exception e2) {
            e = e2;
        }

        //make sure bufferedWriter is closed
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (Exception e2) {
            if (e == null)
                e = e2; 
            //else ignore the error (the first one is more important)
        }

        //and delete partial file if error 
        if (e != null) {
            throw e;
        }       
    }


    /** This returns the minimum value that can be held by this class. */
    public String MINEST_VALUE() {return "\u0000";}

    /** This returns the maximum value that can be held by this class 
        (not including the cohort missing value). */
    public String MAXEST_VALUE() {return "\uFFFE";}

    /**
     * This returns the current capacity (number of elements) of the internal data array.
     * 
     * @return the current capacity (number of elements) of the internal data array.
     */
    public int capacity() {
        return array.length;
    }

    /**
     * This returns the hashcode for this StringArray (dependent only on values,
     * not capacity).
     * WARNING: the algorithm used may change in future versions.
     *
     * @return the hashcode for this byteArray (dependent only on values,
     * not capacity)
     */
    public int hashCode() {
        //see https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode()
        //and https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
        int code = 0;
        for (int i = 0; i < size; i++) 
            code = 31*code + array[i].hashCode();
        return code;
    }

    /**
     * This makes a new subset of this PrimitiveArray based on startIndex, stride,
     * and stopIndex.
     *
     * @param pa the pa to be filled (may be null). If not null, must be of same type as this class. 
     * @param startIndex must be a valid index
     * @param stride   must be at least 1
     * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
     * @return The same pa (or a new PrimitiveArray if it was null) with the desired subset.
     *    If new, it will have a backing array with a capacity equal to its size.
     *    If stopIndex &lt; startIndex, this returns PrimitiveArray with size=0;
     */
    public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
        if (pa != null)
            pa.clear();
        if (startIndex < 0)
            throw new IndexOutOfBoundsException(MessageFormat.format(
                ArraySubsetStart, getClass().getSimpleName(), "" + startIndex));
        if (stride < 1)
            throw new IllegalArgumentException(MessageFormat.format(
                ArraySubsetStride, getClass().getSimpleName(), "" + stride));
        if (stopIndex >= size)
            stopIndex = size - 1;
        if (stopIndex < startIndex)
            return pa == null? new StringArray(new String[0]) : pa;

        int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
        StringArray sa = null;  //for the results
        if (pa == null) {
            sa = new StringArray(willFind, true);
        } else {
            sa = (StringArray)pa;
            sa.ensureCapacity(willFind);
            sa.size = willFind;
        }
        StringHolder tar[] = sa.array;
        if (stride == 1) {
            System.arraycopy(array, startIndex, tar, 0, willFind);
        } else {
            int po = 0;
            for (int i = startIndex; i <= stopIndex; i+=stride) 
                tar[po++] = array[i];
        }
        return sa;
    }

    /**
     * This returns the class (String.class) of the element type.
     *
     * @return the class (String.class) of the element type.
     */
    public Class elementClass() {
        return String.class;
    }

    /**
     * This returns the class index (CLASS_INDEX_STRING) of the element type.
     *
     * @return the class index (CLASS_INDEX_STRING) of the element type.
     */
    public int elementClassIndex() {
        return CLASS_INDEX_STRING;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     */
    public void add(String value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = value == null?        String2.STRING_HOLDER_NULL : //quick, saves time
                        value.length() == 0?  String2.STRING_HOLDER_ZERO :
                        String2.canonicalStringHolder(new StringHolder(value));
    }

    /* *   //CURRENTLY NOT NEEDED and because it is tightly coupled with how this class is currently implemented.
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     * /
    public void add(StringHolder value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = value == null? String2.STRING_HOLDER_NULL : //quick, saves time
                        String2.canonicalStringHolder(value);
    } 

    / **
     * Use this for temporary arrays to add an item to the array (increasing 'size' by 1)
     * without using String2.canonical.
     *
     * @param value the value to be added to the array
     * /
    public void addNotCanonical(StringHolder value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        //still do most common canonicallization
        array[size++] = value == null? String2.STRING_HOLDER_NULL : value;
    } */
 
    /**
     * Use this for temporary arrays to add an item to the array (increasing 'size' by 1)
     * without using String2.canonical.
     *
     * @param value the value to be added to the array
     */
    public void addNotCanonical(String value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        //still do most common canonicallization
        array[size++] = value == null?        String2.STRING_HOLDER_NULL :
                        value.length() == 0?  String2.STRING_HOLDER_ZERO :
                                              new StringHolder(value);
    }

    /**
     * This trims each of the strings.
     *
     */
    public void trimAll() {
        for (int i = 0; i < size; i++) {
            String s = get(i);
            String st = s.trim();
            if (st.length() < s.length())
                set(i, st);
        }
    }

    /**
     * This trims the end of each of the strings.
     *
     */
    public void trimEndAll() {
        for (int i = 0; i < size; i++) {
            String s = get(i);
            String st = String2.trimEnd(s);
            if (st.length() < s.length())
                set(i, st);
        }
    }

    /**
     * This makes sure all of the values are the canonical values.
     *
     */
    public void makeCanonical() {
        for (int i = 0; i < size; i++)
            array[i] = String2.canonicalStringHolder(array[i]);
    }

    /**
     * This adds all the strings from sar.
     *
     * @param sar a String[]
     */
    public void add(String sar[]) {
        int otherSize = sar.length; 
        ensureCapacity(size + (long)otherSize);
        for (int i = 0; i < otherSize; i++)
            add(sar[i]);
    }    

    /**
     * This adds n copies of value to the array (increasing 'size' by n).
     *
     * @param n  if less than 0, this throws Exception
     * @param value the value to be added to the array.
     */
    public void addN(int n, String value) {
        if (n == 0) return;
        if (n < 0)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAddN, getClass().getSimpleName(), "" + n));
        StringHolder sh = String2.canonicalStringHolder(new StringHolder(value));
        ensureCapacity(size + (long)n);
        Arrays.fill(array, size, size + n, sh);
        size += n;
    }

    /**
     * This inserts an item into the array at the specified index, 
     * pushing subsequent items to oldIndex+1 and increasing 'size' by 1.
     *
     * @param index the position where the value should be inserted.
     * @param value the value to be inserted into the array
     */
    public void atInsert(int index, String value) {
        if (index < 0 || index > size)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAtInsert, getClass().getSimpleName(), "" + index, "" + size));
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        System.arraycopy(array, index, array, index + 1, size - index);
        size++;
        set(index, value);
    }

    /**
     * This inserts an item into the array at the specified index, 
     * pushing subsequent items to oldIndex+1 and increasing 'size' by 1.
     *
     * @param index 0..
     * @param value the value, as a String.
     */
    public void atInsertString(int index, String value) {
        atInsert(index, value);
    }

    /**
     * This adds n Strings to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a String.
     */
    public void addNStrings(int n, String value) {
        addN(n, value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a String.
     */
    public void addString(String value) {
        add(value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the float value
     */
    public void addFloat(float value) {
        add(Float.isFinite(value)? String.valueOf(value) : "");
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a double.
     */
    public void addDouble(double value) {
        add(Double.isFinite(value)? String.valueOf(value) : "");
    }

    /**
     * This adds n doubles to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a double.
     */
    public void addNDoubles(int n, double value) {
        addN(n, Double.isFinite(value)? String.valueOf(value) : "");
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as an int.
     */
    public void addInt(int value) {
        add(value == Integer.MAX_VALUE? "" : String.valueOf(value));
    }

    /**
     * This adds n ints to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as an int.
     */
    public void addNInts(int n, int value) {
        addN(n, value == Integer.MAX_VALUE? "" : String.valueOf(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a long.
     */
    public void addLong(long value) {
        add(value == Long.MAX_VALUE? "" : String.valueOf(value));
    }

    /**
     * This adds an element from another PrimitiveArray.
     *
     * @param otherPA the source PA
     * @param otherIndex the start index in otherPA
     * @param nValues the number of values to be added
     * @return 'this' for convenience
     */
    public PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex, int nValues) {

        //add from same type
        if (otherPA.elementClass() == elementClass()) {
            if (otherIndex + nValues > otherPA.size)
                throw new IllegalArgumentException(String2.ERROR + 
                    " in StringArray.addFromPA: otherIndex=" + otherIndex + 
                    " + nValues=" + nValues + 
                    " > otherPA.size=" + otherPA.size);
            ensureCapacity(size + nValues);            
            System.arraycopy(((StringArray)otherPA).array, otherIndex, array, size, nValues);
            size += nValues;
            return this;
        }

        //add from different type
        for (int i = 0; i < nValues; i++)
            add(otherPA.getString(otherIndex++)); //does error checking
        return this;
    }

    /**
     * This sets an element from another PrimitiveArray.
     *
     * @param index the index to be set
     * @param otherPA
     * @param otherIndex
     */
    public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
        set(index, otherPA.getString(otherIndex));
    }

    /**
     * This removes the specified element.
     *
     * @param index the element to be removed, 0 ... size-1
     */
    public void remove(int index) {
        if (index >= size)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayRemove, getClass().getSimpleName(), "" + index, "" + size));
        System.arraycopy(array, index + 1, array, index, size - index - 1);
        size--;

        //for object types, nullify the object at the end
        array[size] = null;
    }

    /**
     * This removes the specified range of elements.
     *
     * @param from the first element to be removed, 0 ... size
     * @param to one after the last element to be removed, from ... size
     */
    public void removeRange(int from, int to) {
        if (to > size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.removeRange: to (" + 
                to + ") > size (" + size + ").");
        if (from >= to) {
            if (from == to) 
                return;
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.removeRange: from (" + 
                from + ") > to (" + to + ").");
        }
        System.arraycopy(array, to, array, from, size - to);
        size -= to - from;

        //for object types, nullify the objects at the end
        Arrays.fill(array, size, size + to - from, null);
    }

    /**
     * This removes any/all the null and 0-length strings at the end.
     *
     * @return the new size
     */
    public int removeEmptyAtEnd() {
        int last = size;
        while (last > 0) {
            char[] car = array[last - 1].charArray(); 
            if (car == null || car.length == 0)
                last--;
            else break;
        }
        removeRange(last, size);
        return size;
    }

    /**
     * This removes any/all the 0-length strings.
     *
     * @return the new size
     */
    public int removeIfNothing() {
        int nGood = 0;
        for (int po = 0; po < size; po++) {
            char[] car = array[po].charArray();
            if (car != null && car.length > 0) {
                if (po > nGood)
                    array[nGood] = array[po];
                nGood++;
            }
        }
        removeRange(nGood, size);
        return size;
    }            
            

    /**
     * Moves elements 'first' through 'last' (inclusive)
     *   to 'destination'.
     *
     * @param first  the first to be move
     * @param last  (exclusive)
     * @param destination the destination, can't be in the range 'first+1..last-1'.
     */
    public void move(int first, int last, int destination) {
        String errorIn = String2.ERROR + " in StringArray.move:\n";

        if (first < 0) 
            throw new RuntimeException(errorIn + "first (" + first + ") must be >= 0.");
        if (last < first || last > size)
            throw new RuntimeException( 
                errorIn + "last (" + last + ") must be >= first (" + first + 
                ") and <= size (" + size + ").");
        if (destination < 0 || destination > size)
            throw new RuntimeException( 
                errorIn + "destination (" + destination + 
                ") must be between 0 and size (" + size + ").");
        if (destination > first && destination < last)
            throw new RuntimeException(
              errorIn + "destination (" + destination + ") must be <= first (" + 
              first + ") or >= last (" + last + ").");
        if (first == last || destination == first || destination == last) 
            return; //nothing to do
        //String2.log("move first=" + first + " last=" + last + " dest=" + destination);
        //String2.log("move initial " + String2.toCSSVString(array));

        //store the range to be moved
        int nToMove = last - first;
        StringHolder[] temp = new StringHolder[nToMove];
        System.arraycopy(array, first, temp, 0, nToMove);

        //if moving to left...    (draw diagram to visualize this)
        if (destination < first) {
            System.arraycopy(array, destination, array, destination + nToMove, first - destination);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination, nToMove);
        } else {
            //moving to right
            System.arraycopy(array, last, array, first, destination - last);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination - nToMove, nToMove);
        }
        //String2.log("move done " + String2.toCSSVString(array));


    }

    /**
     * This just keeps the rows for the 'true' values in the bitset.
     * Rows that aren't kept are removed.
     * The resulting PrimitiveArray is compacted (i.e., it has a smaller size()).
     *
     * @param bitset
     */
    public void justKeep(BitSet bitset) {
        int newSize = 0;
        for (int row = 0; row < size; row++) {
            if (bitset.get(row)) 
                array[newSize++] = array[row];
        }
        removeRange(newSize, size);
    }    

    /**
     * This ensures that the capacity is at least 'minCapacity'.
     *
     * @param minCapacity the minimum acceptable capacity.
     *    minCapacity is type long, but &gt;= Integer.MAX_VALUE will throw exception.
     */
    public void ensureCapacity(long minCapacity) {
        if (array.length < minCapacity) {
            //ensure minCapacity is < Integer.MAX_VALUE
            Math2.ensureArraySizeOkay(minCapacity, "StringArray");  
            //caller may know exact number needed, so don't double above 2x current size
            int newCapacity = (int)Math.min(Integer.MAX_VALUE - 1, array.length + (long)array.length); 
            if (newCapacity < minCapacity) 
                newCapacity = (int)minCapacity; //safe since checked above
            Math2.ensureMemoryAvailable(8L * newCapacity, "StringArray"); //8L is guess
            StringHolder[] newArray = new StringHolder[newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray; //do last to minimize concurrency problems
        }
    }

    /**
     * This returns an array (perhaps 'array') which has 'size' elements.
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     */
    public String[] toArray() {
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toArray"); //8L is guess
        String[] tArray = new String[size];
        for (int i = 0; i < size; i++)
            tArray[i] = array[i].string();
        return tArray;
    }
   
    /**
     * This returns a primitive[] (perhaps 'array') which has 'size' 
     * elements.
     *
     * @return a primitive[] (perhaps 'array') which has 'size' elements.
     */
    public Object toObjectArray() {
        return toArray();
    }

    /**
     * This returns a double[] (perhaps 'array') which has 'size' elements.
     *
     * @return a double[] (perhaps 'array') which has 'size' elements.
     *    Non-finite values are returned as Double.NaN's.
     */
    public double[] toDoubleArray() {
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toDoubleArray"); 
        double dar[] = new double[size];
        for (int i = 0; i < size; i++)
            dar[i] = String2.parseDouble(get(i));
        return dar;
    }

    /**
     * This returns a String[] which has 'size' elements.
     *
     * @return a String[] which has 'size' elements.
     */
    public String[] toStringArray() {
        return toArray();
    }

    /**
     * This gets a specified element.
     *
     * @param index 0 ... size-1
     */
    public String get(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.get: index (" + 
                index + ") >= size (" + size + ").");
        return array[index].string();
    }

    /**
     * This sets a specified element.
     *
     * @param index 0 ... size-1
     * @param value the value for that element
     */
    public void set(int index, String value) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.set: index (" + 
                index + ") >= size (" + size + ").");
        array[index] = String2.canonicalStringHolder(new StringHolder(value));
    }


    /**
     * Return a value from the array as an int.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as an int. This uses String2.parseInt.
     */
    public int getInt(int index) {
        return String2.parseInt(get(index));
    }

    //getRawInt(index) uses default getInt(index) since no smaller data types

    /**
     * Set a value in the array as an int.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. 
     */
    public void setInt(int index, int i) {
        set(index, i == Integer.MAX_VALUE? "" : String.valueOf(i));
    }

    /**
     * Return a value from the array as a long.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a long. This uses String2.parseLong.
     */
    public long getLong(int index) {
        return String2.parseLong(get(index));
    }

    /**
     * Set a value in the array as a long.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. 
     */
    public void setLong(int index, long i) {
        set(index, i == Long.MAX_VALUE? "" : String.valueOf(i));
    }


    /**
     * Return a value from the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a float. String values are parsed
     *   with String2.parseFloat and so may return Float.NaN.
     */
    public float getFloat(int index) {
        return String2.parseFloat(get(index));
    }

    /**
     * Set a value in the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToString(d).
     */
    public void setFloat(int index, float d) {
        set(index, Float.isFinite(d)? String.valueOf(d) : "");
    }

    /**
     * Return a value from the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     */
    public double getDouble(int index) {
        return String2.parseDouble(get(index));
    }

    /**
     * Set a value in the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToString(d).
     */
    public void setDouble(int index, double d) {
        set(index, Double.isFinite(d)? String.valueOf(d) : "");
    }

    /**
     * Return a value from the array as a String.
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns array[index].
     */
    public String getString(int index) {
        return get(index);
    }

    /**
     * Return a value from the array as a String suitable for a JSON file. 
     * char returns a String with 1 character.
     * String returns a json String with chars above 127 encoded as \\udddd.
     * 
     * @param index the index number 0 ... size-1 
     * @return For numeric types, this returns ("" + ar[index]), or null for NaN or infinity.
     */
    public String getJsonString(int index) {
        return String2.toJson(get(index));
    }

    /**
     * Return a value from the array as a String suitable for the data section 
     * of an NCCSV file.
     * 
     * @param index the index number 0 ... size-1 
     * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity.
     *   CharArray and StringArray overwrite this.
     */
    public String getNccsvDataString(int index) {
        return String2.toNccsvDataString(get(index));
    }

    /**
     * Return a value from the array as a String suitable for the data section 
     * of an tsv file, e.g., z \t \u0000 , \".
     * 
     * @param index the index number 0 ... size-1 
     * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity.
     *   CharArray and StringArray overwrite this.
     */
    public String getTsvString(int index) {
        String s = get(index);
        if (s == null)
            return String2.EMPTY_STRING;
        s = String2.toJson(s);
        return s.substring(1, s.length() - 1); //remove enclosing quotes
    }

    /**
     * Set a value in the array as a String.
     * 
     * @param index the index number 0 .. 
     * @param s the value. For numeric PrimitiveArray's, it is parsed
     *   with String2.parse and narrowed if needed by methods like
     *   Math2.roundToString(d).
     */
    public void setString(int index, String s) {
        set(index, s);
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(String lookFor, int startIndex) {
        if (lookFor == null || startIndex >= size)
            return -1;
        char[] lookForc = lookFor.toCharArray();
        for (int i = startIndex; i < size; i++) 
            if (Arrays.equals(array[i].charArray(), lookForc)) //could use == if assume canonical; it's okay if either/both c[] are null
                return i;
        return -1;
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 0, ignoring case.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOfIgnoreCase(String lookFor) {
        return indexOfIgnoreCase(lookFor, 0);
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex', ignoring case.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOfIgnoreCase(String lookFor, int startIndex) {
        if (lookFor == null)
            return -1;
        lookFor = lookFor.toLowerCase();
        for (int i = startIndex; i < size; i++) 
            if (get(i).toLowerCase().equals(lookFor)) 
                return i;
        return -1;
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(String lookFor, int startIndex) {
        if (lookFor == null)
            return -1;
        if (startIndex >= size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.get: startIndex (" + 
                startIndex + ") >= size (" + size + ").");
        char[] lookForc = lookFor.toCharArray();
        for (int i = startIndex; i >= 0; i--) 
            if (Arrays.equals(array[i].charArray(), lookForc)) //could use == if assume canonical. it's okay if either/both b[] are null
                return i;
        return -1;
    }


    /**
     * This finds the first value which has the substring 'lookFor',
     * starting at index 'startIndex' and position 'startPo'.
     *
     * @param lookFor the value to be looked for
     * @param start int[2] {0=startIndex 0 ... size-1, 
     *     1=startPo 0... (used on the first line only; startPo=0 is used thereafter)}
     * @return The results are returned in start and here (for convenience), 
     *     [0]=index, [1]=po, where 'lookFor' is found, or {-1,-1} if not found.
     */
    public int[] indexWith(String lookFor, int start[]) {
        if (lookFor != null) {
            int startPo = start[1];
            for (int i = start[0]; i < size; i++) {
                int po = get(i).indexOf(lookFor, startPo);
                if (po >= 0) {
                    start[0] = i;
                    start[1] = po;
                    return start;
                }
                startPo = 0;
            }
        }
        start[0] = -1;
        start[1] = -1;
        return start;
    }
    /**
     * A simpler form of indexWith that finds the first matching line.
     */
    public int lineContaining(String lookFor) {
        return indexWith(lookFor, new int[]{0, 0})[0];
    }


    /**
     * If size != capacity, this makes a new 'array' of size 'size'
     * so capacity will equal size.
     */
    public void trimToSize() {
        if (size == array.length)
            return;
        StringHolder[] newArray = new StringHolder[size];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }

    /**
     * Test if o is an StringArray with the same size and values.
     *
     * @param o
     * @return true if equal.  o=null returns false.
     */
    public boolean equals(Object o) {
        if (!(o instanceof StringArray)) //handles o==null
            return false;
        StringArray other = (StringArray)o;
        if (other.size() != size)
            return false;
        for (int i = 0; i < size; i++)
            if (!array[i].equals(other.array[i])) //could use == if assume canonical
                return false;
        return true;
    }

    /**
     * Test if o is an StringArray with the same size and values,
     * but returns a String describing the difference (or "" if equal).
     *
     * @param o
     * @return a String describing the difference (or "" if equal).
     *   o=null throws an exception.
     */
    public String testEquals(Object o) {
        if (!(o instanceof StringArray))
            return "The two objects aren't equal: this object is a StringArray; the other is a " + 
                o.getClass().getName() + ".";
        StringArray other = (StringArray)o;
        if (other.size() != size)
            return "The two StringArrays aren't equal: one has " + size + 
               " value(s); the other has " + other.size() + " value(s).";
        for (int i = 0; i < size; i++)
            if (!array[i].equals(other.array[i]))
                return "The two StringArrays aren't equal: this[" + i + "]=\"" + get(i) + 
                                                     "\"; other[" + i + "]=\"" + other.get(i) + "\".";
        return "";
    }


    /** 
     * This converts the elements into an JSON attribute String, e.g.,: -128b, 127b
     * There is no trailing \n.
     * Strings are handled specially: make a newline-separated string, then encode it.
     *
     * @return an NCCSV attribute String
     */
    public String toNccsvAttString() {
        return String2.toNccsvAttString(String2.toSVString(toArray(), "\n", false));  
    }

    /**
     * This returns a JSON-style comma-separated-value list of the elements.
     * CharArray and StringArray overwrite this.
     *
     * @return a csv string of the elements.
     */
    public String toJsonCsvString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(String2.toJson(get(i))); //only null returns null
        }
        return sb.toString();
    }


    /** 
     * This converts the elements into a newline-separated String.
     * There is a trailing newline!
     *
     * @return the newline-separated String representation of the elements
     */
    public String toNewlineString() {
        return String2.toNewlineString(toArray()); //toArray() get just 'size' elements
    }

    /** 
     * This sorts the elements in ascending order.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     */
    public void sort() {
        Arrays.sort(array, 0, size, stringHolderComparator);
    }

    /** 
     * This sorts the elements in ascending order regardless of the case of the letters.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     * This is more sophisticated than Java's String.CASE_INSENSITIVE_ORDER.
     * E.g., all charAt(0) A's will sort by for all charAt(0) a's  (e.g., AA, Aa, aA, aa).
     */
    public void sortIgnoreCase() {
        Arrays.sort(array, 0, size, stringHolderComparatorIgnoreCase);
    }

    /**
     * This compares the values in row1 and row2 for SortComparator,
     * and returns a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     * Currently, this does not checking of the range of index1 and index2,
     * so the caller should be careful.
     * Currently this uses String.compareTo, which may not be the desired comparison,
     * but which is easy to mimic in other situations.
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "array[index1] - array[index2]".
     */
    public int compare(int index1, int index2) {
        return array[index1].compareTo(array[index2]);
    }

    /**
     * This is like compare(), except for StringArray it is caseInsensitive.
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return  a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     */
    public int compareIgnoreCase(int index1, int index2) {
        StringHolder s1 = array[index1];
        StringHolder s2 = array[index2];
        int c = s1.compareToIgnoreCase(s2);
        if (c != 0) 
            return c;
        return s1.compareTo(s2);
    }


    /**
     * This copies the value in row 'from' to row 'to'.
     * This does not check that 'from' and 'to' are valid;
     * the caller should be careful.
     * The value for 'from' is unchanged.
     *
     * @param from an index number 0 ... size-1
     * @param to an index number 0 ... size-1
     */
    public void copy(int from, int to) {
        array[to] = array[from];
    }

    /**
     * This reorders the values in 'array' based on rank.
     *
     * @param rank is an int with values (0 ... size-1) 
     * which points to the row number for a row with a specific 
     * rank (e.g., rank[0] is the row number of the first item 
     * in the sorted list, rank[1] is the row number of the
     * second item in the sorted list, ...).
     */
    public void reorder(int rank[]) {
        int n = rank.length;
        //new length could be n, but I'll keep it the same array.length as before
        StringHolder[] newArray = new StringHolder[array.length]; 
        for (int i = 0; i < n; i++)
            newArray[i] = array[rank[i]];
        array = newArray;
    }

    /**
     * This reverses the order of the bytes in each value,
     * e.g., if the data was read from a little-endian source.
     */
    public void reverseBytes() {
        //StringArray does nothing because insensitive to big/little-endian
    }

    /**
     * This writes 'size' elements to a DataOutputStream.
     *
     * @param dos the DataOutputStream
     * @return the number of bytes used per element (for Strings, this is
     *    the size of one of the strings, not others, and so is useless;
     *    for other types the value is consistent).
     *    But if size=0, this returns 0.
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos) throws Exception {
        for (int i = 0; i < size; i++)
            dos.writeUTF(get(i)); 
        return size == 0? 0 : 10; //generic answer
    }

    /**
     * This writes one element to a DataOutputStream via writeUTF.
     *
     * @param dos the DataOutputStream
     * @param i the index of the element to be written
     * @return the number of bytes used for this element
     *    (for Strings, this varies; for others it is consistent)
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos, int i) throws Exception {
        int po = dos.size();
        dos.writeUTF(get(i));
        return dos.size() - po;
    }

    /**
     * This reads/adds n elements from a DataInputStream.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    public void readDis(DataInputStream dis, int n) throws Exception {
        ensureCapacity(size + (long)n);
        for (int i = 0; i < n; i++)
            add(dis.readUTF());
    }

    /**
     * This writes a short with the classIndex() of the PA, an int with the 'size',
     * then the elements to a DataOutputStream.
     * Only StringArray overwrites this.
     *
     * @param dos the DataOutputStream
     * @throws IOException if trouble
     */
/* project not finished or tested
    public void writeNccsvDos(DataOutputStream dos) throws Exception {
        dos.writeShort(elementClassIndex()); 
        dos.writeInt(size);
        for (int i = 0; i < size; i++) 
            String2.writeNccsvDos(dos, get(i));
    }
*/
    /**
     * This writes one element to an NCCSV DataOutputStream.
     * Only StringArray overwrites this.
     *
     * @param dos the DataOutputStream
     * @throws Exception if trouble
     */
/* project not finished or tested
    public void writeNccsvDos(DataOutputStream dos, int i) throws Exception {
        String2.writeNccsvDos(dos, get(i)); 
    }
*/

    /**
     * This writes one String to a DataOutputStream in the format DODS
     * wants (see www.opendap.org DAP 2.0 standard, section 7.3.2.1).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     * Just 8 bits are stored: there is no utf or other unicode support.
     * See DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit.
     * Ah: dods.dap.DString reader assumes ISO-8859-1, which is first page of unicode.
     *
     * @param dos
     * @param s
     * @throws Exception if trouble
     */
    public static void externalizeForDODS(DataOutputStream dos, String s) throws Exception {
        int n = s.length();
        dos.writeInt(n); //for Strings, just write size once
        for (int i = 0; i < n; i++) { //just low 8 bits written; no utf or other unicode support, 
            char c = s.charAt(i);  //2016-11-29 I added: char>255 -> '?', it's better than low 8 bits
            dos.writeByte(c < 256? c : '?'); //dods.dap.DString reader assumes ISO-8859-1, which is first page of unicode
        }

        //pad to 4 bytes boundary at end
        while (n++ % 4 != 0)
            dos.writeByte(0);
    }

    /**
     * This writes all the data to a DataOutputStream in the
     * DODS Array format (see www.opendap.org DAP 2.0 standard, section 7.3.2.1).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     *
     * @param dos
     * @throws Exception if trouble
     */
    public void externalizeForDODS(DataOutputStream dos) throws Exception {
        dos.writeInt(size);
        dos.writeInt(size); //yes, a second time
        for (int i = 0; i < size; i++)
            externalizeForDODS(dos, get(i));
    }

    /**
     * This writes one element to a DataOutputStream in the
     * DODS Atomic-type format (see www.opendap.org DAP 2.0 standard, section 7.3.2).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     *
     * @param dos
     * @param i the index of the element to be written
     * @throws Exception if trouble
     */
    public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
        externalizeForDODS(dos, get(i));
    }

    /**
     * This reads/appends String values from a StringArray from a DODS DataInputStream,
     * and is thus the complement of externalizeForDODS.
     *
     * @param dis
     * @throws IOException if trouble
     */
    public void internalizeFromDODS(DataInputStream dis) throws java.io.IOException {
        int nStrings = dis.readInt();
        ensureCapacity(size + (long)nStrings);
        dis.readInt(); //skip duplicate of nStrings
        byte buffer[] = new byte[80];
        for (int i = 0; i < nStrings; i++) {
            int nChar = dis.readInt(); 
            if (buffer.length < nChar)
                buffer = new byte[nChar + 10];
            dis.readFully(buffer, 0, nChar);
            add(new String(buffer, 0, nChar));

            //pad to 4 bytes boundary at end
            while (nChar++ % 4 != 0)
                dis.readByte();
        }
    }

    /**
     * This reads one value from a randomAccessFile.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array
     * @param index the index of the desired value (0..)
     * @param nBytesPer is the number of bytes per string
     * @return the requested value as a double
     * @throws Exception if trouble
     */
    public static String rafReadString(RandomAccessFile raf, long start, int index, 
        int nBytesPer) throws Exception {

        raf.seek(start + nBytesPer * index);
        byte bar[] = new byte[nBytesPer];
        raf.readFully(bar);      
        int po = 0;
        while (po < nBytesPer && bar[po] != 0) 
            po++;
        return new String(bar, 0, po);
    }

    /**
     * This appends the data in another pa to the current data.
     *
     * @param pa 
     */
    public void append(PrimitiveArray pa) {
        int otherSize = pa.size(); 
        ensureCapacity(size + (long)otherSize);
        if (pa instanceof StringArray) {
            System.arraycopy(((StringArray)pa).array, 0, array, size, otherSize);
            size += otherSize; 
        //2017-04-06 this was contemplated, but better to handle this some other way, 
        //  e.g., CharArray.getString()
        //} else if (pa instanceof CharArray) { //for Argo
        //    CharArray ca = (CharArray)pa;
        //    for (int i = 0; i < otherSize; i++) {
        //        char ch = ca.get(i);
        //        array[size + i] = String2.canonicalStringHolder(ch == Character.MAX_VALUE? "" : ch + ""); 
        //    }
        } else {
            for (int i = 0; i < otherSize; i++)
                add(pa.getString(i)); //this converts mv's
        }
    }    

    /**
     * This appends the data in another pa to the current data.
     * This "raw" variant leaves missingValue from integer data types 
     * (e.g., ByteArray missingValue=127) AS IS (e.g., "127").
     *
     * @param pa the pa to be appended
     */
    public void rawAppend(PrimitiveArray pa) {
        int otherSize = pa.size(); 
        ensureCapacity(size + (long)otherSize);
        if (pa instanceof StringArray) {
            System.arraycopy(((StringArray)pa).array, 0, array, size, otherSize);
            size += otherSize; //do last to minimize concurrency problems
        } else {
            for (int i = 0; i < otherSize; i++)
                add(pa.getRawString(i)); //this DOESN'T convert mv's
        }
    }    

    /**
     * This returns the length of the longest String.
     *
     * @return the length of the longest String
     */
    public int maxStringLength() {
        int max = 0;
        for (int i = 0; i < size; i++) {
            String s = get(i);
            max = Math.max(max, s == null? 0 : s.length());
        }
        return max;
    }

    /**
     * This populates 'indices' with the indices (ranks) of the values in this StringArray
     * (ties get the same index). For example, "d", "d", "", "c" returns 1,1,2,0.
     * !!!Currently this uses native sort, so lower case sorts before uppercase;
     * except "" is ranked at end (like missing value).
     *
     * @param indices the intArray that will capture the indices of the values 
     *  (ties get the same index). For example, "d", "d", "", "c" returns 1,1,2,0.
     * @return a PrimitveArray (the same type as this class) with the distinct/unique values, sorted.
     *     If all the values are unique and already sorted, this returns 'this'.
     */
    public PrimitiveArray makeIndices(IntArray indices) {
        indices.clear();
        if (size == 0) {
            return new StringArray();
        }

        //make a hashMap with all the unique values (associated values are initially all dummy)
        Integer dummy = new Integer(-1);
        HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
        String lastValue = get(0); //since lastValue often equals currentValue, cache it
        hashMap.put(lastValue, dummy);   //special for String
        boolean alreadySorted = true;
        for (int i = 1; i < size; i++) {
            String currentValue = get(i);
            int compare = lastValue.compareTo(currentValue); //special for String,    read "is bigger than"
            if (compare != 0) {    //special for String
                if (compare > 0)   //special for String
                    alreadySorted = false;
                lastValue = currentValue;
                hashMap.put(lastValue, dummy);
            }
        }

        //quickly deal with: all unique and already sorted
        Set keySet = hashMap.keySet();
        int nUnique = keySet.size();
        if (nUnique == size && alreadySorted) {
            indices.ensureCapacity(size);
            for (int i = 0; i < size; i++)
                indices.add(i);           
            //String2.log("StringArray.makeIndices all unique and already sorted.");
            return this; //the PrimitiveArray with unique values
        }

        //store all the elements in an array
        String unique[] = new String[nUnique];
        Iterator iterator = keySet.iterator();
        int count = 0;
        while (iterator.hasNext())
            unique[count++] = (String)iterator.next();
        if (nUnique != count)
            throw new RuntimeException("StringArray.makeRankArray nUnique(" + nUnique +
                ") != count(" + count + ")!");

        //sort them
        Arrays.sort(unique); //a variant could use String2.STRING_COMPARATOR_IGNORE_CASE);

        //special for StringArray: "" (missing value) sorts highest
        if (((String)unique[0]).length() == 0) {
            System.arraycopy(unique, 1, unique, 0, nUnique - 1);
            unique[nUnique - 1] = "";
        }

        //put the unique values back in the hashMap with the ranks as the associated values
        for (int i = 0; i < count; i++)
            hashMap.put(unique[i], new Integer(i));

        //convert original values to ranks
        int ranks[] = new int[size];
        lastValue = get(0);
        ranks[0] = ((Integer)hashMap.get(lastValue)).intValue();
        int lastRank = ranks[0];
        for (int i = 1; i < size; i++) {
            if (get(i).equals(lastValue)) { 
                ranks[i] = lastRank;
            } else {
                lastValue = get(i);
                ranks[i] = ((Integer)hashMap.get(lastValue)).intValue();
                lastRank = ranks[i];
            }
        }

        //store the results in ranked
        indices.append(new IntArray(ranks));

        return new StringArray(unique);

    }

    /**
     * This changes all instances of the first value to the second value.
     *
     * @param from the original value (use "" (not "NaN") for standard missingValue)
     * @param to   the new value (use "" (not "NaN") for standard missingValue)
     * @return the number of values switched
     */
    public int switchFromTo(String from, String to) {
        if (from.equals(to))
            return 0;
        char[] fromc = from.toCharArray();
        StringHolder tosh = String2.canonicalStringHolder(new StringHolder(to));
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (Arrays.equals(array[i].charArray(), fromc)) {  //could be == if assume all elements are canonical
                array[i] = tosh;
                count++;
            }
        }
        return count;
    }

    /**
     * This makes a StringArray with the words and double-quoted phrases from searchFor
     * (which are separated by white space; commas are treated like any other whitespace).
     *
     * @param searchFor
     * @return a StringArray with the words and phrases (no longer double quoted) from searchFor
     *   (which are separated by white space; commas are treated like any other whitespace).
     *   Interior double quotes in double-quoted phrases must be doubled 
     *   (e.g., "a quote "" within a phrase").
     *   The resulting parts are all trim'd.
     */
    public static StringArray wordsAndQuotedPhrases(String searchFor) {
        StringArray sa = new StringArray();
        if (searchFor == null)
            return sa;
        int po = 0;
        int n = searchFor.length();
        while (po < n) {
            char ch = searchFor.charAt(po);
            if (ch == '"') {
                //a phrase (a quoted string)
                int po2 = po + 1;
                while (po2 < n) {
                    if (searchFor.charAt(po2) == '"') {
                        //is it 2 double quotes?
                        if (po2 + 1 < n && searchFor.charAt(po2+1) == '"') 
                            po2 += 2; //yes, so continue
                        else break; //no, it's the end quote
                    } else {
                        po2++;
                    }
                }
                String s = searchFor.substring(po + 1, po2);
                sa.add(String2.replaceAll(s, "\"\"", "\""));
                po = po2 + 1;
            } else if (String2.isWhite(ch) || ch == ',') {
                //whitespace or comma
                po++;
            } else {
                //a word
                int po2 = po + 1;
                while (po2 < searchFor.length() && 
                    !String2.isWhite(searchFor.charAt(po2)) &&
                    searchFor.charAt(po2) != ',')
                    po2++;
                //String2.log("searchFor=" + searchFor + " wordPo=" + po + " po2=" + po2);
                sa.add(searchFor.substring(po, po2));
                po = po2;
            }
        }
        return sa;
    }


    /**
     * This makes a StringArray with the comma-separated words and double-quoted phrases from searchFor.
     * <br>The double-quoted phrases can have internal double quotes encoded as "" or \".
     * <br>null becomes sa.length() == 0.
     * <br>"" becomes sa.length() == 0.
     * <br>" " becomes sa.length() == 1.
     *
     * @param searchFor
     * @return a StringArray with the words and double-quoted phrases from searchFor.
     *    The items are trim'd.
     */
    public static StringArray fromCSV(String searchFor) {
        return new StringArray(arrayFromCSV(searchFor, ","));
    }
    public static StringArray fromCSV(String searchFor, String separatorChars) {
        return new StringArray(arrayFromCSV(searchFor, separatorChars));
    }

    /**
     * This is like fromCSV, but with any "" elements removed.
     *
     * @param searchFor
     * @return a StringArray with the words and double-quoted phrases from searchFor.
     *    The items are trim'd.
     */
    public static StringArray fromCSVNoBlanks(String searchFor) {
        String[] sar = arrayFromCSV(searchFor);
        int tSize = sar.length;
        StringArray sa = new StringArray();
        for (int i = 0; i < tSize; i++)
            if (sar[i].length() > 0)
                sa.add(sar[i]);
        return sa;
    }

    /**
     * This makes a String[] with the comma-separated words and double-quoted phrases from searchFor.
     * <br>This avoids String2.canonical(to), so will be faster if just parsing then discarding
     *   or storing in some other data structure.
     *
     * <p>The double-quoted phrases can have internal double quotes encoded as "" or \".
     * <br>null becomes sa.length() == 0.
     * <br>"" becomes sa.length() == 0.
     * <br>" " becomes sa.length() == 1.
     *
     * @param searchFor
     * @return a String[] with the words and double-quoted phrases from searchFor.
     *    The items are trim'd.
     *   <br>Note that null and "null" return the word "null". No returned element will be null.
     *   <br>backslashed characters are converted to the special character (e.g., double quotes or newline).
     *   <br>An element may be nothing.
     */
    public static String[] arrayFromCSV(String searchFor) {
        return arrayFromCSV(searchFor, ",", true, true); //trim, keepNothing
    }    

    /**
     * This variant of arrayFromCSV lets you specify the separator chars (e.g., "," or
     * ",;" and which trims each result string.
     */
    public static String[] arrayFromCSV(String searchFor, String separatorChars) {
        return arrayFromCSV(searchFor, separatorChars, true, true); //trim, keepNothing
    }

    /**
     * This variant of arrayFromCSV lets you specify the separator chars (e.g., "," or
     * ",;").
     *
     * @param trim If true, each results string is trimmed.
     */
    public static String[] arrayFromCSV(String searchFor, String separatorChars,
        boolean trim) {
        return arrayFromCSV(searchFor, separatorChars, trim, true);  //keepNothing?
    }

        
    /**
     * This variant of arrayFromCSV lets you specify the separator chars (e.g., "," or
     * ",;") and whether to trim the strings, and whether to keep "" elements.
     *
     * @param trim If true, each results string is trimmed.
     */
    public static String[] arrayFromCSV(String searchFor, String separatorChars,
        boolean trim, boolean keepNothing) {
        if (searchFor == null)
            return new String[0];
        //String2.log(">> arrayFrom s=" + String2.annotatedString(searchFor));
        ArrayList<String> al = new ArrayList();
        int po = 0; //next char to be looked at
        StringBuilder word = new StringBuilder();
        int n = searchFor.length();
        while (po < n) {
            //String2.log(">> arrayFrom po=" + po + " al.size=" + al.size() + " word=" + word);
            char ch = searchFor.charAt(po++);

            if (ch == '"') {
                //a quoted string
                if (word.length() == 0)
                    word.append('\u0000'); //indicate there is something; it will be trimmed later

                int start = po;
                if (po < n) {
                    while (true) {
                        ch = searchFor.charAt(po++);
                        //String2.log(">> quoteloop ch=" + ch);
                        // "" internal quote
                        if (ch == '"' && po < n && searchFor.charAt(po) == '"') {
                            word.append(searchFor.substring(start, po - 1));
                            start = po++;  //the 2nd char " will be the first appended later

                        // backslashed character
                        } else if (ch == '\\' && po < n) {
                            word.append(searchFor.substring(start, po - 1));
                            ch = searchFor.charAt(po++);
                            //don't support \\b, it's trouble
                            if      (ch == 'f') word.append('\f');
                            else if (ch == 't') word.append('\t');
                            else if (ch == 'n') word.append('\n');
                            else if (ch == 'r') word.append('\r');
                            else if (ch == '\'') word.append('\'');
                            else if (ch == '\"') word.append('\"');
                            else if (ch == '\\') word.append('\\');
                            else if (ch == 'u' && po <= n-4 &&  // \\uxxxx
                                String2.isHexString(searchFor.substring(po, po + 4))) {
                                word.append((char)(Integer.parseInt(searchFor.substring(po, po + 4), 16)));
                                po += 4;
                                }
                            //else if (ch == '') word.append('');  
                            else word.append("\\" + ch); //or just ch?
                            start = po;  //next char will be the first appended later
                            if (po == n)
                                break;

                        // the end of the quoted string?
                        } else if (ch == '"') { 
                            word.append(searchFor.substring(start, po - 1));
                            break;

                        // the end of searchFor?
                        } else if (po == n) { 
                            word.append(searchFor.substring(start, po));
                            break;

                        // a letter in the quoted string
                        //} else {
                        //    word.append(ch);
                        }
                    }
                }

            //end of word?
            } else if (separatorChars.indexOf(ch) >= 0) { //e.g., comma or semicolon
                String s = word.toString();
                if (trim) {
                    s = s.trim();  //trim gets rid of all whitespace, including \n and \\u0000
                } else {
                    s = String2.replaceAll(s, "\u0000", "");
                }
                if (s.length() > 0 || keepNothing)
                    al.add(s); 
                word.setLength(0);
                word.append('\u0000'); //indicate there is something

            //a character
            } else {
                word.append(ch);
            }
        }
        if (word.length() > 0) {
            String s = word.toString();
            if (trim) {
                s = s.trim();  //trim gets rid of all whitespace, including \n and \\u0000
            } else {
                s = String2.replaceAll(s, "\u0000", "");
            }
            if (s.length() > 0 || keepNothing)
                al.add(s); 
        }
        return al.toArray(new String[0]);
    }

    
    /**
     * This makes a StringArray from the comma separated list of strings in csv. 
     * If a string has an internal comma or double quotes, it must have double quotes 
     * at the beginning and end and the internal double quotes must be doubled; otherwise it doesn't.
     *
     * @param csv  e.g., "He said, ""Hi"".", 2nd phrase, "3rd phrase"
     * @return a StringArray with the strings (trimmed) from csv.
     *   csv=null returns StringArray of length 0.
     *   csv="" returns StringArray of length 0.
     */
    /* not bad, but doesn't support \" encoding of internal quote
    public static StringArray fromCSV(String csv) {
        StringArray sa = new StringArray();
        if (csv == null || csv.length() == 0)
            return sa;
        int n = csv.length();
        if (n == 0)
            return sa;
        int po = 0;
        boolean something = false;

        while (po < n) {
            char ch = csv.charAt(po);
            if (ch == ' ') {
                po++;
                something = true;
            } else if (ch == ',') {
                sa.add("");
                po++;
                something = true;
            } else if (ch == '"') {
                //a quoted string
                int po2 = po + 1;
                while (po2 < n) {
                    if (csv.charAt(po2) == '"') {
                        //is it 2 double quotes?
                        if (po2 + 1 < n && csv.charAt(po2+1) == '"') 
                            po2 += 2; //yes, so continue
                        else break; //no, it's the end quote; what if next thing isn't a comma???
                    } else {
                        po2++;
                    }
                }
                String s = csv.substring(po + 1, po2);
                sa.add(String2.replaceAll(s, "\"\"", "\""));

                //should be only spaces till next comma
                //whatever it is, trash it
                po = po2 + 1;
                while (po < n && csv.charAt(po) != ',')
                    po++;
                if (po < n && csv.charAt(po) == ',') {
                    po++; something = true;
                } else {po = n; something = false;
                }
            } else {
                //an unquoted string
                int po2 = csv.indexOf(',', po + 1);
                if (po2 >= 0) {something = true;
                } else {po2 = n; something = false;
                }
                sa.add(csv.substring(po, po2).trim());
                po = po2 + 1;
            }
        }
        if (something)
            sa.add("");
        return sa;
    } */

    /**
     * This is a purposely <strong>simple</strong>, 2-double-quotes-aware, backslash-aware, splitter
     * that makes an ArrayString from the items in an NCCSV-style string.
     *
     * <br>The elements won't be canonical, so will be faster if just parsing 
     *   then discarding or storing in some other data structure.
     *
     * <p>Strings should be JSON-like, but \char are left as-is (not converted
     *   to special char) and 2 double quotes are still 2 double quotes.  
     * <br>null becomes sa.length() == 0.
     * <br>"" becomes sa.length() == 1.
     *
     * @param csv
     * @return a StringArray with the items.
     *   <br>Quoted strings are still in quoted strings. 
     *   <br>Backslashed characters are not converted to the special character 
     *     (e.g., double quotes or newline).
     *   <br>Items are trimmed.
     */
    public static StringArray simpleFromNccsv(String csv) {
        StringArray sa = new StringArray();
        if (csv == null)
            return sa;
        int start = 0; //start of this item
        int po = 0; //next char to be looked at
        int n = csv.length();
        while (po < n) {
            char ch = csv.charAt(po++);

            if (ch == '"') {
                while (po < n) {
                    ch = csv.charAt(po++);
                    if (ch == '\\' && po < n) {
                        po++;
                        continue;
                    } else if (ch == '"') {
                        //matching close quote
                        break;
                    }
                }

            } else if (ch == '\\' && po < n) {
                po++;
                continue;

            } else if (ch == ',') {
                //end of item
                sa.addNotCanonical(csv.substring(start, po - 1).trim()); //avoid canonical
                start = po;
            }
        }
        sa.addNotCanonical(csv.substring(start, po).trim()); //avoid canonical
        return sa;
    }

    /**
     * This is a purposely <strong>simple</strong> JSON array parser.
     *
     * <br>The elements won't be canonical, so will be faster if just parsing 
     *   then discarding or storing in some other data structure.
     *
     * <p>Strings should be JSON strings (double quoted, backslash escaped)
     * but are left as-is.
     * <br>null becomes sa.length() == 0.
     * <br>"" becomes sa.length() == 1.
     *
     * @param csv  This must start with [ and end with ]. 
     * @return a StringArray with the items.
     *   <br>Quoted strings are still in quoted strings. 
     *   <br>Backslashed characters are not converted to the special character 
     *     (e.g., double quotes or newline).
     *   <br>Items are trimmed.
     *   <br>throws SimpleException if not valid JSON array (not thorough checking though)
     */
    public static StringArray simpleFromJsonArray(String csv) {
        if (csv == null)
            throw new SimpleException("A null value isn't a valid JSON array.");

        //the first non-white char must be [
        int n = csv.length();
        int po = 0; //next char to be looked at
        while (po < n && String2.isWhite(csv.charAt(po)))
            po++;
        if (po == n || csv.charAt(po++) != '[')
            throw new SimpleException("A JSON array must start with '['.");

        //is it an empty array?
        StringArray sa = new StringArray();
        while (po < n && String2.isWhite(csv.charAt(po)))
            po++;
        if (po < n && csv.charAt(po) == ']')
            return sa;

        //collect the items
        int start = po; //start of current item
        while (po < n) {
            char ch = csv.charAt(po++);

            if (ch == '"') {
                while (true) {
                    if (po >= n)
                        throw new SimpleException(
                            "A string in the JSON array lacks a closing double quote.");
                    ch = csv.charAt(po++);
                    if (ch == '\\') { //if there is no next char, that will be caught
                        po++; //eat the next char
                        continue;
                    } else if (ch == '"') {
                        //matching close quote
                        break;
                    }
                }


            } else if (ch == ',' || ch == ']') {
                //end of item
                //it must be something
                String s = csv.substring(start, po - 1).trim();
                if (s.length() == 0)
                    throw new SimpleException(
                        "A value in a JSON array must not be nothing.");

                sa.addNotCanonical(s); 
                start = po;

                if (ch == ']') {
                    //the rest must be whitespace
                    while (po < n) {
                        if (!String2.isWhite(csv.charAt(po++)))
                            throw new SimpleException(
                                "There must not be content in the JSON array after ']'.");
                    }
                    return sa;
                }
            }
        }
        throw new SimpleException(
            "A JSON array must end with ']'.");
    }

    /**
     * This tests if the values in the array are sorted in ascending order (tied is ok).
     * The details of this test are geared toward determining if the 
     * values are suitable for binarySearch.
     *
     * @return "" if the values in the array are sorted in ascending order (or tied);
     *   or an error message if not (i.e., if descending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A null value returns an error message (but "" is ok).
     */
    public String isAscending() {
        if (size == 0)
            return "";
        String s = get(0);
        if (s == null) 
            return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                "[0]=null");
        for (int i = 1; i < size; i++) {
            String oldS = s;
            s = get(i);
            if (s == null)
                return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                    "[" + i + "]=null");
            if (oldS.compareTo(s) > 0) {
                return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                    "[" + (i-1) + "]=" + String2.toJson(get(i-1)) + 
                     " > [" + i + "]=" + String2.toJson(get(i)));
            }
        }
        return "";
    }

    /**
     * This tests if the values in the array are sorted in descending order (tied is ok).
     *
     * @return "" if the values in the array are sorted in descending order (or tied);
     *   or an error message if not (i.e., if ascending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A null value returns an error message (but "" is ok).
     */
    public String isDescending() {
        if (size == 0)
            return "";
        String s = get(0);
        if (s == null) 
            return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                "[0]=null");
        for (int i = 1; i < size; i++) {
            String oldS = s;
            s = get(i);
            if (s == null)
                return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                    "[" + i + "]=null");
            if (oldS.compareTo(s) < 0) {
                return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                    "[" + (i-1) + "]=" + String2.toJson(get(i-1)) + 
                     " < [" + i + "]=" + String2.toJson(get(i)));
            }
        }
        return "";
    }

    /**
     * This tests for adjacent tied values and returns the index of the first tied value.
     * Adjacent NaNs are treated as ties.
     *
     * @return the index of the first tied value (or -1 if none).
     */
    public int firstTie() {
        for (int i = 1; i < size; i++) {
            if (Arrays.equals(array[i - 1].charArray(), array[i].charArray())) { //either or both can be null
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * This tests if the values in the array are evenly spaced (ascending or descending)
     * (via Math2.almostEqual(9)).
     * This is rarely used because numbers are usually stored in numeric
     * XXXArrays.
     *
     * @return "" if the values in the array are evenly spaced;
     *   or an error message if not.
     *   If size is 0 or 1, this returns "".
     */
    public String isEvenlySpaced() {
        if (size <= 2)
            return "";
        //average is closer to exact than first diff
        //and usually detects not-evenly-spaced anywhere in the array on first test!
        double average = (getDouble(size - 1) - getDouble(0)) / (size - 1.0); 
        for (int i = 1; i < size; i++) {
            if (!Math2.almostEqual(9, getDouble(i) - getDouble(i - 1), average)) {
                return MessageFormat.format(ArrayNotEvenlySpaced, getClass().getSimpleName(),
                    "" + (i - 1), "" + getDouble(i - 1), "" + i, "" + getDouble(i),
                    "" + (getDouble(i) - getDouble(i-1)), "" + average);
            }
        }
        return "";
    }

    /** Thie replaces any instances of 'from' with 'to' within each string. */
    public void intraReplaceAll(String from, String to) {
        for (int i = 0; i < size; i++) {
            String s = get(i);
            if (s != null) 
                set(i, String2.replaceAll(s, from, to));
        }
    }

    /** Thie replaces any instances of 'from' with 'to' within each string, 
     * regardless of 'from's case in the string. */
    public void intraReplaceAllIgnoreCase(String from, String to) {
        for (int i = 0; i < size; i++) {
            String s = get(i);
            if (s != null) 
                set(i, String2.replaceAllIgnoreCase(s, from, to));
        }
    }

    /**
     * This finds the number of non-missing values, and the index of the min and
     *    max value.
     *
     * @return int[3], [0]=the number of non-missing values, 
     *    [1]=index of min value (if tie, index of last found; -1 if all mv),
     *    [2]=index of max value (if tie, index of last found; -1 if all mv).
     */
    public int[] getNMinMaxIndex() {
        int n = 0, tmini = -1, tmaxi = -1;
        String tmin = "\uFFFF";
        String tmax = "\u0000";
        for (int i = 0; i < size; i++) {
            String s = get(i);
            if (s != null && s.length() > 0) {
                n++;
                if (s.compareTo(tmin) <= 0) {tmini = i; tmin = s; }
                if (s.compareTo(tmax) >= 0) {tmaxi = i; tmax = s; }
            }
        }
        return new int[]{n, tmini, tmaxi};
    }

    /**
     * This compares two text files, line by line, and throws Exception indicating 
     * line where different.
     * nullString == nullString is ok.
     *  
     * @param fileName1 a complete file name
     * @param fileName2 a complete file name
     * @throws Exception if files are different
     */
    public static void diff(String fileName1, String fileName2) throws Exception {
        StringArray sa1 = fromFile(fileName1);
        StringArray sa2 = fromFile(fileName2);
        sa1.diff(sa2);
    }

    /**
     * This repeatedly compares two text files, line by line, and throws Exception indicating 
     * line where different.
     * nullString == nullString is ok.
     *  
     * @param fileName1 a complete file name
     * @param fileName2 a complete file name
     * @throws Exception if files are different
     */
    public static void repeatedDiff(String fileName1, String fileName2) throws Exception {
        while (true) {
            try {
                String2.log("\nComparing " + fileName1 + 
                            "\n      and " + fileName2);
                StringArray sa1 = fromFile(fileName1);
                StringArray sa2 = fromFile(fileName2);
                sa1.diff(sa2);
                String2.log("!!! The files are the same!!!");
                break;
            } catch (Exception e) {
                String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
                    "\nPress ^C to stop or Enter to compare the files again..."); 
            }
        }
    }

    /** 
     * This returns the values in this StringArray in a HashSet.
     */
    public HashSet<String> toHashSet() {
        HashSet<String> hs = new HashSet(Math.max(8, size * 4 / 3));
        for (int i = 0; i < size; i++)
            hs.add(get(i));
        return hs;
    }

    /** 
     * This adds the values in hs to this StringArray and returns this StringArray for convenience.
     * The order of the elements in this StringArray is not specified.
     */
    public StringArray addHashSet(HashSet hs) {
        for (Object o : hs)
            add(o.toString());
        return this;
    }

    /**
     * This returns the index of the first value that doesn't match the regex.
     *
     * @param regex
     * @return the index of the first value that doesn't match the regex, or -1 if they all match.
     * @throws RuntimeException if regex won't compile.
     */
    public int firstNonMatch(String regex) {
        return firstNonMatch(Pattern.compile(regex));
    }

    /**
     * This returns the index of the first value that doesn't match the regex pattern p.
     *
     * @param p
     * @return the index of the first value that doesn't match the regex pattern p, or -1 if they all match.
     */
    public int firstNonMatch(Pattern p) {
        for (int i = 0; i < size; i++) {
            String s = get(i);
            if (s == null || !p.matcher(s).matches())
                return i;                
        }
        return -1;
    }

    /**
     * This converts all !String2.isSomething2(s) strings to "".
     *
     * @return the number of non-"" elements converted.
     */
    public int convertIsSomething2() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            char[] car = array[i].charArray();
            if (car == null || (car.length > 0 && !String2.isSomething2(get(i)))) {
                array[i] = String2.STRING_HOLDER_ZERO; 
                count++; 
            }
        }
        return count;
    }



    /**
     * This tests the methods of this class.
     *
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable{
        String2.log("*** Testing StringArray");
/* for releases, this line should have open/close comment */
        String sar[];
        StringArray anArray = new StringArray();

        StringArray sa1 = null;

        Test.ensureEqual("test".equals(null),             false, "");
        Test.ensureEqual(sa1 instanceof StringArray,      false, "");
        Test.ensureEqual((new StringArray()).equals(sa1), false, "");

        //** test default constructor and many of the methods
        Test.ensureEqual(anArray.isIntegerType(), false, "");
        Test.ensureEqual(anArray.missingValue(), Double.NaN, "");
        anArray.addString("");
        Test.ensureEqual(anArray.get(0),               "", "");
        Test.ensureEqual(anArray.getRawInt(0),         Integer.MAX_VALUE, "");
        Test.ensureEqual(anArray.getRawDouble(0),      Double.NaN, "");
        Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
        Test.ensureEqual(anArray.getRawString(0),      "", "");
        Test.ensureEqual(anArray.getRawNiceDouble(0),  Double.NaN, "");
        Test.ensureEqual(anArray.getInt(0),            Integer.MAX_VALUE, "");
        Test.ensureEqual(anArray.getDouble(0),         Double.NaN, "");
        Test.ensureEqual(anArray.getString(0), "", "");
        anArray.clear();

        //unsignedFactory, which uses unsignedAppend
        anArray = (StringArray)unsignedFactory(String.class, 
            new ByteArray(new byte[] {0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), "0.0, 1.0, 127.0, 128.0, 255.0", "");
        anArray.clear();        

        anArray = (StringArray)unsignedFactory(String.class, 
            new CharArray(new char[] {(char)0, (char)1, '\u7FFF', '\u8000', '\uFFFF'}));
        Test.ensureEqual(anArray.toString(), "0.0, 1.0, 32767.0, 32768.0, 65535.0", "");
        anArray.clear();        

        anArray = (StringArray)unsignedFactory(String.class, 
            new ShortArray(new short[] {0, 1, Short.MAX_VALUE, Short.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), "0.0, 1.0, 32767.0, 32768.0, 65535.0", "");
        anArray.clear();        

        anArray = (StringArray)unsignedFactory(String.class, 
            new IntArray(new int[] {0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), 
            // 0, 1,    2147483647,    2147483648,    4294967295
            "0.0, 1.0, 2.147483647E9, 2.147483648E9, 4.294967295E9", ""); //precise
        anArray.clear();        

        anArray = (StringArray)unsignedFactory(String.class, 
            new LongArray(new long[] {0, 1, Long.MAX_VALUE, Long.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), 
            "0.0, 1.0, 9.223372036854776E18, 9.223372036854776E18, 1.8446744073709552E19", ""); //rounded/imprecise
        anArray.clear();        

        anArray = (StringArray)unsignedFactory(String.class, 
            new FloatArray(new float[] {0, 1, Float.MAX_VALUE, -Float.MAX_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), 
            "0.0, 1.0, 3.4028235E38, -3.4028235E38, -1.0", ""); 
        anArray.clear();        

        Test.ensureEqual(anArray.size(), 0, "");
        anArray.add("1234.5");
        Test.ensureEqual(anArray.size(), 1, "");
        Test.ensureEqual(anArray.get(0), "1234.5", "");
        Test.ensureEqual(anArray.getInt(0), 1235, "");
        Test.ensureEqual(anArray.getFloat(0), 1234.5f, "");
        Test.ensureEqual(anArray.getDouble(0), 1234.5, "");
        Test.ensureEqual(anArray.getString(0), "1234.5", "");
        Test.ensureEqual(anArray.elementClass(), String.class, "");
        String tArray[] = anArray.toArray();
        Test.ensureEqual(tArray, new String[]{"1234.5"}, "");

        //intentional errors
        try {anArray.get(1);              throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).", "");
        }
        try {anArray.set(1, "100");         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getInt(1);           throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setInt(1, 100);      throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getLong(1);          throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setLong(1, 100);     throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getFloat(1);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setFloat(1, 100);    throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getDouble(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setDouble(1, 100);   throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getString(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setString(1, "100"); throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).", "");
        }

        //set NaN returned as NaN
        anArray.setDouble(0, Double.NaN);   Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, -1e300);       Test.ensureEqual(anArray.getDouble(0), -1e300, ""); 
        anArray.setDouble(0, 2.2);          Test.ensureEqual(anArray.getDouble(0), 2.2,          ""); 
        anArray.setFloat( 0, Float.NaN);    Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, -1e33f);       Test.ensureEqual(anArray.getFloat(0),  -1e33f,  ""); 
        anArray.setFloat( 0, 3.3f);         Test.ensureEqual(anArray.getFloat(0),  3.3f,          ""); 
        anArray.setLong(0, Long.MAX_VALUE); Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 9123456789L);    Test.ensureEqual(anArray.getLong(0),   9123456789L, ""); 
        anArray.setLong(0, 4);              Test.ensureEqual(anArray.getLong(0),   4, ""); 
        anArray.setInt(0,Integer.MAX_VALUE);Test.ensureEqual(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 1123456789);      Test.ensureEqual(anArray.getInt(0),    1123456789, ""); 
        anArray.setInt(0, 5);               Test.ensureEqual(anArray.getInt(0),    5, ""); 

        //** test capacity constructor, test expansion, test clear
        anArray = new StringArray(2, false);
        Test.ensureEqual(anArray.size(), 0, "");
        for (int i = 0; i < 10; i++) {
            anArray.add(String.valueOf(i));   
            Test.ensureEqual(anArray.get(i), "" + i, "");
            Test.ensureEqual(anArray.size(), i+1, "");
        }
        Test.ensureEqual(anArray.size(), 10, "");
        anArray.clear();
        Test.ensureEqual(anArray.size(), 0, "");

        //active
        anArray = new StringArray(3, true);
        Test.ensureEqual(anArray.size(), 3, "");
        Test.ensureEqual(anArray.get(2), "", "");

        //** test array constructor
        anArray = new StringArray(new String[]{"0","2","4","6","8"});
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1), "2", "");
        Test.ensureEqual(anArray.get(2), "4", "");
        Test.ensureEqual(anArray.get(3), "6", "");
        Test.ensureEqual(anArray.get(4), "8", "");

        //test compare
        Test.ensureEqual(anArray.compare(1, 3), -4, "");
        Test.ensureEqual(anArray.compare(1, 1),  0, "");
        Test.ensureEqual(anArray.compare(3, 1),  4, "");

        //test compareIgnoreCase
        StringArray cic = fromCSV("A, a, ABE, abe");
        Test.ensureEqual(cic.compare(0, 1), -32, "");
        Test.ensureEqual(cic.compare(1, 2), 32, "");
        Test.ensureEqual(cic.compare(2, 3), -32, "");
        Test.ensureEqual(cic.compare(1, 3), -2, "");

        Test.ensureEqual(cic.compareIgnoreCase(0, 1), -32, "");
        Test.ensureEqual(cic.compareIgnoreCase(1, 2), -2, "");
        Test.ensureEqual(cic.compareIgnoreCase(2, 3), -32, "");
        Test.ensureEqual(cic.compareIgnoreCase(1, 3), -2, "");

        //test toString
        Test.ensureEqual(anArray.toString(), "0, 2, 4, 6, 8", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{5, 0, 4}, "");
        Test.ensureEqual(anArray.getNMinMax(), new String[]{"5", "0", "8"}, "");

        //test calculateStats
        anArray.addString("");
        double stats[] = anArray.calculateStats();
        anArray.remove(5);
        Test.ensureEqual(stats[STATS_N], 5, "");
        Test.ensureEqual(stats[STATS_MIN], 0, "");
        Test.ensureEqual(stats[STATS_MAX], 8, "");
        Test.ensureEqual(stats[STATS_SUM], 20, "");

        //test indexOf(int) indexOf(String)
        Test.ensureEqual(anArray.indexOf(null),   -1, "");
        Test.ensureEqual(anArray.indexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.indexOf("0", 1), -1, "");
        Test.ensureEqual(anArray.indexOf("8", 0),  4, "");
        Test.ensureEqual(anArray.indexOf("9", 0), -1, "");

        Test.ensureEqual(anArray.indexOfIgnoreCase(null),   -1, "");
        Test.ensureEqual(anArray.indexOfIgnoreCase("8", 0),  4, "");

        //test lastIndexOf
        Test.ensureEqual(anArray.lastIndexOf(null),   -1, "");
        Test.ensureEqual(anArray.lastIndexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.lastIndexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.lastIndexOf("8", 4),  4, "");
        Test.ensureEqual(anArray.lastIndexOf("6", 2), -1, "");
        Test.ensureEqual(anArray.lastIndexOf("6", 3),  3, "");
        Test.ensureEqual(anArray.lastIndexOf("9", 2), -1, "");

        //test remove
        anArray.remove(1);
        Test.ensureEqual(anArray.size(), 4, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1), "4", "");
        Test.ensureEqual(anArray.get(3), "8", "");
        Test.ensureEqual(anArray.array[4], null, ""); //can't use get()

        //test atInsert(index, value)    and maxStringLength
        anArray.atInsertString(1, "22");
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1),"22", "");
        Test.ensureEqual(anArray.get(2), "4", "");
        Test.ensureEqual(anArray.get(4), "8", "");
        Test.ensureEqual(anArray.maxStringLength(), 2, "");
        anArray.remove(1);
        Test.ensureEqual(anArray.maxStringLength(), 1, "");

        //test removeRange
        anArray.removeRange(4, 4); //make sure it is allowed
        anArray.removeRange(1, 3);
        Test.ensureEqual(anArray.size(), 2, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1), "8", "");
        Test.ensureEqual(anArray.array[2], null, ""); //can't use get()
        Test.ensureEqual(anArray.array[3], null, "");

        //test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
        Test.ensureEqual(anArray.toString(), "0, 8", "");
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");
        Test.ensureEqual(anArray.toStringArray(), new String[]{"0", "8"}, "");

        //test trimToSize
        anArray.trimToSize();
        Test.ensureEqual(anArray.array.length, 2, "");

        //test equals
        StringArray anArray2 = new StringArray();
        anArray2.add("0"); 
        Test.ensureEqual(anArray.testEquals("A String"), 
            "The two objects aren't equal: this object is a StringArray; the other is a java.lang.String.", "");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two StringArrays aren't equal: one has 2 value(s); the other has 1 value(s).", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.addString("7");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two StringArrays aren't equal: this[1]=\"8\"; other[1]=\"7\".", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.setString(1, "8");
        Test.ensureEqual(anArray.testEquals(anArray2), "", "");
        Test.ensureTrue(anArray.equals(anArray2), "");

        //test toObjectArray
        Test.ensureEqual(anArray.toArray(), anArray.toObjectArray(), "");

        //test toDoubleArray
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");

        //test reorder
        int rank[] = {1, 0};
        anArray.reorder(rank);
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{8, 0}, "");

        //** test append and clone
        anArray = new StringArray(new String[]{"1"});
        anArray.append(new ByteArray(new byte[]{5, -5}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5}, "");
        anArray.append(new StringArray(new String[]{"a", "9"}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");
        anArray2 = (StringArray)anArray.clone();
        Test.ensureEqual(anArray2.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");

        //test move
        anArray = new StringArray(new String[]{"0","1","2","3","4"});
        anArray.move(1,3,0);
        Test.ensureEqual(anArray.toArray(), new String[]{"1","2","0","3","4"}, "");

        anArray = new StringArray(new String[]{"0","1","2","3","4"});
        anArray.move(1,2,4);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","2","3","1","4"}, "");

        //test reorder
        anArray.reverse();
        Test.ensureEqual(anArray.toArray(), new String[]{"4","1","3","2","0"}, "");


        //move does nothing, but is allowed
        anArray = new StringArray(new String[]{"0","1","2","3","4"});
        anArray.move(1,1,0);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(1,2,1);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(1,2,2);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(5,5,0);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(3,5,5);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");

        //makeIndices
        anArray = new StringArray(new String[] {"d", "a", "a", "b"});
        IntArray indices = new IntArray();
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "a, b, d", "");
        Test.ensureEqual(indices.toString(),  "2, 0, 0, 1", "");

        anArray = new StringArray(new String[] {"d", "d", "a", "", "b",});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "a, b, d, ", "");
        Test.ensureEqual(indices.toString(), "2, 2, 0, 3, 1", "");

        anArray = new StringArray(new String[] {"aa", "ab", "ac", "ad"});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "aa, ab, ac, ad", "");
        Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

        //switchToFakeMissingValue
        anArray = new StringArray(new String[] {"", "1", "2", "", "3", ""});
        Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
        Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
        anArray.switchFromTo("75", "");
        Test.ensureEqual(anArray.toString(), ", 1, 2, , 3, ", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 1, 4}, "");

        //addN
        anArray = new StringArray(new String[] {"a"});
        anArray.addN(2, "bb");
        Test.ensureEqual(anArray.toString(), "a, bb, bb", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 0, 2}, "");

        //add array
        anArray.add(new String[]{"17", "19"});
        Test.ensureEqual(anArray.toString(), "a, bb, bb, 17, 19", "");

        //subset
        PrimitiveArray ss = anArray.subset(1, 3, 4);
        Test.ensureEqual(ss.toString(), "bb, 19", "");
        ss = anArray.subset(0, 1, 0);
        Test.ensureEqual(ss.toString(), "a", "");
        ss = anArray.subset(0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        ss = anArray.subset(1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        ss.trimToSize();
        anArray.subset(ss, 1, 3, 4);
        Test.ensureEqual(ss.toString(), "bb, 19", "");
        anArray.subset(ss, 0, 1, 0);
        Test.ensureEqual(ss.toString(), "a", "");
        anArray.subset(ss, 0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        anArray.subset(ss, 1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        //wordsAndQuotedPhrases(String searchFor)
        Test.ensureEqual(wordsAndQuotedPhrases(null).toString(), "", "");
        Test.ensureEqual(wordsAndQuotedPhrases("a bb").toString(), "a, bb", "");
        Test.ensureEqual(wordsAndQuotedPhrases(" a bb c ").toString(), "a, bb, c", "");
        Test.ensureEqual(wordsAndQuotedPhrases(",a,bb, c ,d").toString(), "a, bb, c, d", "");
        Test.ensureEqual(wordsAndQuotedPhrases(" a,\"b b\",c ").toString(), "a, b b, c", "");
        Test.ensureEqual(wordsAndQuotedPhrases(" a,\"b b").toString(), "a, b b", ""); //no error for missing "
        Test.ensureEqual(wordsAndQuotedPhrases(" , ").toString(), "", "");
        anArray = wordsAndQuotedPhrases(" a,\"b\"\"b\",c "); //internal quotes
        Test.ensureEqual(anArray.toString(), "a, \"b\"\"b\", c", "");
        Test.ensureEqual(anArray.get(1), "b\"b", "");
        anArray = wordsAndQuotedPhrases(" a \"b\"\"b\" c "); //internal quotes
        Test.ensureEqual(anArray.toString(), "a, \"b\"\"b\", c", "");
        Test.ensureEqual(anArray.get(1), "b\"b", "");
        anArray = wordsAndQuotedPhrases("a \"-bob\" c");
        Test.ensureEqual(anArray.get(1), "-bob", "");
        anArray = wordsAndQuotedPhrases("a -\"bob\" c"); //internal quotes
        Test.ensureEqual(anArray.get(1), "-\"bob\"", "");

        //fromCSV(String searchFor)
        Test.ensureEqual(fromCSV(null).toString(), "", "");
        Test.ensureEqual(fromCSV("a, b b").toString(), "a, b b", "");
        Test.ensureEqual(fromCSV(" a, b b ,c ").toString(), "a, b b, c", "");
        Test.ensureEqual(fromCSV(",a,b b, c ,d,").toString(), ", a, b b, c, d, ", "");
        Test.ensureEqual(fromCSV(" a, \"b b\" ,c ").toString(), "a, b b, c", "");
        Test.ensureEqual(fromCSV(" a,\"b b").toString(), "a, b b", ""); //no error for missing "
        Test.ensureEqual(fromCSV(" , ").toString(), ", ", "");
        anArray = fromCSV(" a,\"b\"\"b\",c "); Test.ensureEqual(anArray.get(1), "b\"b", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\\"b\",c "); Test.ensureEqual(anArray.get(1), "b\"b", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\tb\",c ");  Test.ensureEqual(anArray.get(1), "b\tb", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\nb\",c ");  Test.ensureEqual(anArray.get(1), "b\nb", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\'b\",c ");  Test.ensureEqual(anArray.get(1), "b\'b", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\\"b\",c "); Test.ensureEqual(anArray.get(1), "b\"b", ""); //internal quotes
        anArray = fromCSV(" a \"b\"\"b\" c "); Test.ensureEqual(anArray.get(0), "a b\"b c", ""); //internal quotes

        //evenlySpaced
        anArray = new StringArray(new String[] {"10","20","30"});
        Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
        anArray.set(2, "30.1");
        Test.ensureEqual(anArray.isEvenlySpaced(), 
            "StringArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.05.", "");
        Test.ensureEqual(anArray.smallestBiggestSpacing(),
            "  smallest spacing=10.0: [0]=10.0, [1]=20.0\n" +
            "  biggest  spacing=10.100000000000001: [1]=20.0, [2]=30.1", "");

        //fromCSV
        Test.ensureEqual(fromCSV(null).toString(), "", "");
        Test.ensureEqual(fromCSV("").toString(), "", "");
        Test.ensureEqual(fromCSV(" ").toString(), "", "");
        Test.ensureEqual(fromCSV(",").toString(), ", ", "");
        Test.ensureEqual(fromCSV(" , ").toString(), ", ", "");
        Test.ensureEqual(fromCSV("a,bb").toString(), "a, bb", "");
        Test.ensureEqual(fromCSV(" a , bb ").toString(), "a, bb", "");
        Test.ensureEqual(fromCSV(" a, bb ,c ").toString(), "a, bb, c", "");
        Test.ensureEqual(fromCSV(",a,bb, c ,").toString(), ", a, bb, c, ", "");
        Test.ensureEqual(fromCSV(" a,\"b b\",c ").toString(), "a, b b, c", "");
        Test.ensureEqual(fromCSV(" a,\"b b").toString(), "a, b b", ""); //no error for missing "
        Test.ensureEqual(fromCSV(" a,\"b \"\"\"\"b\"junk,c ").toString(), "a, \"b \"\"\"\"bjunk\", c", "");
        Test.ensureEqual(fromCSV(" a,\"b \"\"\"\"b\"junk,c ").get(1), "b \"\"bjunk", "");
        Test.ensureEqual(fromCSV(" a,\"b,b\"junk").toString(), "a, \"b,bjunk\"", "");

        //isAscending
        anArray = new StringArray(new String[] {"go","go","hi"});
        Test.ensureEqual(anArray.isAscending(), "", "");
        anArray.set(2, null);
        Test.ensureEqual(anArray.isAscending(), 
            "StringArray isn't sorted in ascending order: [2]=null.", "");
        anArray.set(1, "ga");
        Test.ensureEqual(anArray.isAscending(), 
            "StringArray isn't sorted in ascending order: [0]=\"go\" > [1]=\"ga\".", "");

        //isDescending
        anArray = new StringArray(new String[] {"hi", "go", "go"});
        Test.ensureEqual(anArray.isDescending(), "", "");
        anArray.set(2, null);
        Test.ensureEqual(anArray.isDescending(), 
            "StringArray isn't sorted in descending order: [2]=null.", "");
        anArray.set(1, "pa");
        Test.ensureEqual(anArray.isDescending(), 
            "StringArray isn't sorted in descending order: [0]=\"hi\" < [1]=\"pa\".", "");

        //firstTie
        anArray = new StringArray(new String[] {"hi", "pa", "go"});
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(1, "hi");
        Test.ensureEqual(anArray.firstTie(), 0, "");
        anArray.set(1, null);
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(2, null);
        Test.ensureEqual(anArray.firstTie(), 1, "");

        //diff
        anArray  = new StringArray(new String[] {"0", "11", "22"});
        anArray2 = new StringArray(new String[] {"0", "11", "22"});
        String s = anArray.diffString(anArray2);  Test.ensureEqual(s, "", "s=" + s);
        anArray2.add("33");
        s = anArray.diffString(anArray2);  Test.ensureEqual(s, "  old [3]=33,\n  new [3]=null.", "s=" + s);
        anArray2.set(2, "23");
        s = anArray.diffString(anArray2);  Test.ensureEqual(s, "  old [2]=23,\n  new [2]=22.", "s=" + s);
        IntArray ia = new IntArray(new int[]{0, 11, 22});
        s = anArray.diffString(ia);  Test.ensureEqual(s, "", "s=" + s);
        ia.set(2, 23);
        s = anArray.diffString(ia);  Test.ensureEqual(s, "  old [2]=23,\n  new [2]=22.", "s=" + s);

        //utf8
        String os = " s\\\n\tÃ\u20ac ";
        StringArray sa = new StringArray(new String[]{os});
        sa.toUTF8().fromUTF8();
        Test.ensureEqual(sa.get(0), os, "");

        //hashcode
        anArray = new StringArray();
        for (int i = 5; i < 1000; i++)
            anArray.add("" + i);
        String2.log("hashcode1=" + anArray.hashCode());
        anArray2 = (StringArray)anArray.clone();
        Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
        anArray.atInsertString(0, "2");
        Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

        //justKeep
        BitSet bitset = new BitSet();
        anArray = new StringArray(new String[] {"0", "11", "22", "33", "44"});
        bitset.set(1);
        bitset.set(4);
        anArray.justKeep(bitset);
        Test.ensureEqual(anArray.toString(), "11, 44", "");

        //min max
        anArray = new StringArray();
        anArray.addString(anArray.MINEST_VALUE());
        anArray.addString(anArray.MAXEST_VALUE());
        Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE(), "");
        Test.ensureEqual(anArray.getString(0), "\u0000", "");
        Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE(), "");

        //sort
        anArray = fromCSV("AB, AB, Ab, Ab, ABC, ABc, AbC, aB, aB, ab, ab");
        anArray.sort();
        //note that short sort before long when identical
        Test.ensureEqual(anArray.toString(), "AB, AB, ABC, ABc, Ab, Ab, AbC, aB, aB, ab, ab", "");

        //sortIgnoreCase
        anArray = fromCSV("AB, AB, Ab, Ab, ABC, ABc, AbC, aB, aB, ab, ab");
        anArray.sortIgnoreCase();
        //note that all short ones sort before all long ones
        Test.ensureEqual(anArray.toString(), "AB, AB, Ab, Ab, aB, aB, ab, ab, ABC, ABc, AbC", "");

        //numbers
        DoubleArray da = new DoubleArray(new double[]{5, Double.NaN});
        da.sort();  //do NaN's sort high?
        Test.ensureEqual(da.toString(), "5.0, NaN", "");
        Test.ensureEqual(da.getString(1), "", "");

        //arrayFromCSV, test with unquoted internal strings
        //outside of a quoted string \\u20ac is backslash+...
        sar = arrayFromCSV(" ab , \n\t\\\u00c3\u20ac , \\n\\u20ac , ",                   ",", true); //trim?
        Test.ensureEqual(String2.annotatedString(String2.toCSVString(sar)), 
            "ab,\\[195][8364],\\n\\u20ac,[end]", "");
        sar = arrayFromCSV(" ab , \n\t\\\u00c3\u20ac , \\n\\u20ac , ",                   ",", false); //trim?
        Test.ensureEqual(String2.annotatedString(String2.toCSVString(sar)), 
            " ab , [10]\n[9]\\[195][8364] , \\n\\u20ac , [end]", "");

        //arrayFromCSV, test with quoted internal strings
        //inside of a quoted string \\u20ac is Euro
        sar = arrayFromCSV("\" ab \",\" \n\t\\\u00c3\u20ac \",\" \\n\\u20ac \",\" \"",   ",", true); //trim?
        Test.ensureEqual(String2.annotatedString(String2.toCSVString(sar)), 
            "ab,\\[195][8364],[8364],[end]", "");
        sar = arrayFromCSV("\" ab \",\" \n\t\\\u00c3\u20ac \",\" \\n\\u20ac \",\" \"",   ",", false); //trim?
        Test.ensureEqual(String2.annotatedString(String2.toCSVString(sar)),
            " ab , [10]\n[9]\\[195][8364] , [10]\n[8364] , [end]", "");

        //inCommon
        anArray  = fromCSV("a, b, d");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "a, d", "");

        anArray  = fromCSV("a, d");
        anArray2 = fromCSV("b, e");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "", "");

        anArray  = fromCSV("");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "", "");

        anArray  = fromCSV("");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "", "");

        anArray  = fromCSV("c");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "c", "");

        anArray  = fromCSV("a, b, c");
        anArray2 = fromCSV("c");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "c", "");

        //removeEmpty
        anArray  = fromCSV("a, , c");
        Test.ensureEqual(anArray.removeIfNothing(), 2, "");
        Test.ensureEqual(anArray.toString(), "a, c", "");

        anArray  = fromCSV(" , b, ");
        Test.ensureEqual(anArray.removeIfNothing(), 1, "");
        Test.ensureEqual(anArray.toString(), "b", "");

        anArray  = fromCSV(" , , ");
        Test.ensureEqual(anArray.removeIfNothing(), 0, "");
        Test.ensureEqual(anArray.toString(), "", "");


        //fromCSVNoBlanks
        anArray  = fromCSVNoBlanks(", b, ,d,,");
        Test.ensureEqual(anArray.toString(), "b, d", "");

        //toHashSet   addHashSet
        anArray = fromCSV("a, e, i, o, uu");
        HashSet hs = anArray.toHashSet();
        anArray2 = (new StringArray()).addHashSet(hs);
        anArray2.sort();
        Test.ensureEqual(anArray.toArray(), anArray2.toArray(), "");

        //fromNccsv
        anArray = simpleFromNccsv("");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"\"", "");
        Test.ensureEqual(anArray.size(), 1, "");

        anArray = simpleFromNccsv("a");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"a\"", "");
        Test.ensureEqual(anArray.size(), 1, "");

        anArray = simpleFromNccsv(" a , b ,");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"a\", \"b\", \"\"", "");
        Test.ensureEqual(anArray.size(), 3, "");

        anArray = simpleFromNccsv(" \" a\t\n\b\'z\"\" \" , 1.23f, a\"");
        //  \\b is removed
        Test.ensureEqual(String2.annotatedString(String2.replaceAll(anArray.get(0), "\n", "")),
            "\" a[9][8]'z\"\" \"[end]", "");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"\\\" a\\t\\n'z\\\"\\\" \\\"\", \"1.23f\", \"a\\\"\"", "");
        Test.ensureEqual(anArray.size(), 3, "");

        anArray = simpleFromNccsv(
            // \\b is not allowed
            "'\\f', '\\n', '\\r', '\\t', '\\\\', '\\/', '\\\"', 'a', '~', '\\u00C0', '\\u0000', '\\uffFf'");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"'\\\\f'\", \"'\\\\n'\", \"'\\\\r'\", \"'\\\\t'\", \"'\\\\\\\\'\", \"'\\\\/'\", \"'\\\\\\\"'\", \"'a'\", \"'~'\", \"'\\\\u00C0'\", \"'\\\\u0000'\", \"'\\\\uffFf'\"", 
            anArray.toJsonCsvString());
        Test.ensureEqual(anArray.size(), 12, "");

        //removeEmptyAtEnd();
        anArray = new StringArray(new String[] {"hi", "go", "to"});
        Test.ensureEqual(anArray.size(), 3, "");
        anArray.removeEmptyAtEnd();
        Test.ensureEqual(anArray.size(), 3, "");
        anArray.set(0, "");
        anArray.set(2, "");
        anArray.removeEmptyAtEnd();
        Test.ensureEqual(anArray.size(), 2, "");
        anArray.set(1, "");
        anArray.removeEmptyAtEnd();
        Test.ensureEqual(anArray.size(), 0, "");

        //simpleFromJsonArray
        anArray = simpleFromJsonArray("[]");
        Test.ensureEqual(anArray.size(), 0, "");
        anArray = simpleFromJsonArray("\n[\n]\n");
        Test.ensureEqual(anArray.size(), 0, "");

        anArray = simpleFromJsonArray("[1]");
        Test.ensureEqual(anArray.toJsonCsvString(), "\"1\"", ""); 
        anArray = simpleFromJsonArray("[1,2]");
        Test.ensureEqual(anArray.toJsonCsvString(), "\"1\", \"2\"", ""); 
        anArray = simpleFromJsonArray("[ 1 ]");
        Test.ensureEqual(anArray.toJsonCsvString(), "\"1\"", ""); 
        anArray = simpleFromJsonArray("[ 1 , 2 ]");
        Test.ensureEqual(anArray.toJsonCsvString(), "\"1\", \"2\"", ""); 

        anArray = simpleFromJsonArray("[\"\"]");
        Test.ensureEqual(anArray.toJsonCsvString(), "\"\\\"\\\"\"", ""); 
        anArray = simpleFromJsonArray("[\"\",\"\"]");
        Test.ensureEqual(anArray.toJsonCsvString(), "\"\\\"\\\"\", \"\\\"\\\"\"", ""); 

        anArray = simpleFromJsonArray("[\" a\n\t\f\\\u00C0\u20ac \"]");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"\\\" a\\n\\t\\f\\\\\\u00c0\\u20ac \\\"\"", ""); 
        anArray = simpleFromJsonArray(" [ \" a\n\t\f\\\u00C0\u20ac \" ] ");
        Test.ensureEqual(anArray.toJsonCsvString(), 
            "\"\\\" a\\n\\t\\f\\\\\\u00c0\\u20ac \\\"\"", ""); 

        //test invalid jsonArray
        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray(null);
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: A null value isn't a valid JSON array.", "");

        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray(" ");
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: A JSON array must start with '['.", "");

        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray(" a[1]");
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: A JSON array must start with '['.", "");

        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray(" [1]a");
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: There must not be content in the JSON array after ']'.", "");

        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray("[a,]");
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: A value in a JSON array must not be nothing.", "");

        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray("[\"ab]");
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: A string in the JSON array lacks a closing double quote.", "");

        s = "shouldn't get here";
        try {  anArray = simpleFromJsonArray("[\"ab\\");
        } catch (Exception e) { s = e.toString();
        }
        Test.ensureEqual(s, "com.cohort.util.SimpleException: A string in the JSON array lacks a closing double quote.", "");


        //tryToFindNumericMissingValue() 
        Test.ensureEqual((new StringArray(new String[] {                  })).tryToFindNumericMissingValue(), Double.NaN, "");
        Test.ensureEqual((new StringArray(new String[] {""                })).tryToFindNumericMissingValue(), Double.NaN, "");
        Test.ensureEqual((new StringArray(new String[] {"a", "", "1", "2" })).tryToFindNumericMissingValue(), Double.NaN, "");
        Test.ensureEqual((new StringArray(new String[] {"a", "", "1", "99"})).tryToFindNumericMissingValue(), Double.NaN, ""); //doesn't catch 99, would be nice if it did?

    }

}

