/* 
 * StringObject Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;


/**
 * This class holds a String that can be changed.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-06-28
 *
 */
public class StringObject  {
    public String s;

    /**
     * The constructor.
     *
     * @param initialValue the initial value of s.
     */
    public StringObject(String initialValue) {
        s = initialValue;
    }

    /**
     * Returns the string.
     *
     * @return the string.
     */
    public String toString() {
        return s;
    }
}
