/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter and Contributors.
 *
 *  Contributions made by:
 *
 *  Brett Smith
 *  Richard Pernavas
 *  Erwin Bolwidt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */

// ===========================================================================
// CONTENT  : CLASS StringUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 21/03/2003
// HISTORY  :
//  10/07/1999 	duma  CREATED
//	09/12/1999	duma	added		->	SPACE, repeat()
//										moved		->	from package com.mdcs.util
//	25/01/2000	duma	moved		->	from package com.mdcs.text
//  09/02/2000  duma  changed ->  renamed SPACE to CH_SPACE
//                    added   ->  CH_CR, CH_TAB, ..., STR_SPACE, STR_NEWLINE, ...
//  11/01/2002  duma  added   ->	indexOf(), indexOfIgnoreCase(), contains(), containsIgnoreCase()
//	17/05/2002	duma	added		->	copyFrom()
//	03/07/2002	duma	added		->	cutHead(), prefix(), suffix()
//	06/07/2002	duma	added		->	indexOf() and contains() for StringPattern and reverse()
//	15/08/2002	duma	added		->	upTo(), startingFrom(), asMap()
//	29/09/2002	duma	added		->	allParts() and allSubstrings() that don't skip empty elements
//	06/03/2003	duma	changed	->	append() now uses System.arraycopy()
//										added		->	remove( String[], String[] ), remove( String[], String )
//																removeNull( String[] )
//	21/03/2003	duma	added		->	leftPad(), leftPadCh(), rightPad(), rightPadCh() for int values
//
// Copyright (c) 1999-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package com.sshtools.daemon.util;

import java.io.*;

import java.text.*;

import java.util.*;


/**
 * The sole instance of this class provides several convienience methods for
 * string manipulation such as substring replacement or character repetition.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class StringUtil {
    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Constant for the space character */
    public static final char CH_SPACE = '\u0020';

    /** Constant for the new line character */
    public static final char CH_NEWLINE = '\n';

    /** Constant for the carriage return character */
    public static final char CH_CR = '\r';

    /** Constant for the tabulator character */
    public static final char CH_TAB = '\t';

    /** Constant for the String representation of the space character */
    public static final String STR_SPACE = "\u0020";

    /** Constant for the String representation of the new line character */
    public static final String STR_NEWLINE = "\n";

    /**
 * Constant for the String representation of the carriage return character
 */
    public static final String STR_CR = "\r";

    /** Constant for the String representation of the tabulator character */
    public static final String STR_TAB = "\t";
    private static final String WORD_DELIM = STR_SPACE + STR_TAB + STR_NEWLINE +
        STR_CR;

    // =========================================================================
    // CLASS VARIABLES
    // =========================================================================
    private static StringUtil singleton = null;

    private static StringUtil getSingleton() {
        return singleton;
    }

    private static void setSingleton(StringUtil inst) {
        singleton = inst;
    }

    // =========================================================================
    // PUBLIC CLASS METHODS
    // =========================================================================

    /**
 * Returns the one and only instance of this class.
 *
 * @return
 */
    public static StringUtil current() {
        if (getSingleton() == null) {
            setSingleton(new StringUtil());
        }

        return getSingleton();
    }

    // current()
    // =========================================================================
    // PUBLIC INSTANCE METHODS
    // =========================================================================

    /**
     * Returns the given string with all found oldSubStr replaced by newSubStr. <br>
 * Example: StringUtil.current().replaceAll( "Seven of ten", "even", "ix"
 * ) ;<br>
 * results in: "Six of ten"
 *
 * @param sourceStr The string that should be checked for occurrences of
 *        oldSubStr
 * @param oldSubStr The string that is searched for in sourceStr
 * @param newSubStr The new string that is placed everywhere the oldSubStr
 *        was found
 *
 * @return The original string with all found substrings replaced by new
 *         strings
 */
    public String replaceAll(String sourceStr, String oldSubStr,
        String newSubStr) {
        String part = null;
        String result = "";
        int index = -1;
        int subLen = 0;
        subLen = oldSubStr.length();
        part = sourceStr;

        while ((part.length() > 0) && (subLen > 0)) {
            index = part.indexOf(oldSubStr);

            if (index >= 0) {
                result = result + part.substring(0, index) + newSubStr;
                part = part.substring(index + subLen);
            } else {
                result = result + part;
                part = "";
            }
        }

        // while
        return result;
    }

    // replaceAll()
    // -------------------------------------------------------------------------

    /**
 * Returns a string with size of count and all characters initialized with
 * ch.   <br>
 *
 * @param ch the character to be repeated in the result string.
 * @param count the number of times the given character should occur in the
 *        result string.
 *
 * @return A string containing <i>count</i> characters <i>ch</i>.
 */
    public String repeat(char ch, int count) {
        StringBuffer buffer = null;
        buffer = new StringBuffer(count);

        for (int i = 1; i <= count; i++) {
            buffer.append(ch);
        }

        return (buffer.toString());
    }

    // repeat()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of substrings of the given text.    <br>
 * The delimiters between the substrings are the whitespace characters
 * SPACE, NEWLINE, CR and TAB.
 *
 * @param text The string that should be splitted into whitespace separated
 *        words
 *
 * @return An array of substrings of the given text
 *
 * @see #parts(String, String)
 */
    public String[] words(String text) {
        return this.parts(text, WORD_DELIM);
    }

    // words()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of substrings of the given text.    <br>
 * The separators between the substrings are the given delimiters. Each
 * character in the delimiter string is treated as a separator. <br>
 * All consecutive delimiters are treated as one delimiter, that is there
 * will be no empty strings in the result.
 *
 * @param text The string that should be splitted into substrings
 * @param delimiters All characters that should be recognized as a
 *        separator or substrings
 *
 * @return An array of substrings of the given text
 *
 * @see #allParts(String, String)
 * @see #substrings(String, String)
 * @see #allSubstrings(String, String)
 */
    public String[] parts(String text, String delimiters) {
        return this.parts(text, delimiters, false);
    }

    // parts()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of substrings of the given text.    <br>
 * The separators between the substrings are the given delimiters. Each
 * character in the delimiter string is treated as a separator. <br>
 * For each delimiter that is followed immediately by another delimiter an
 * empty string will be added to the result. There are no empty strings
 * added to the result for a delimiter at the very beginning of at the
 * very end.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * allParts( "/A/B//", "/" )  --> { "A", "B", "" }<br>
 * allParts( "/A,B/C;D", ",;/" )  --> { "A", "B", "C", "D" }<br>
 * allParts( "A/B,C/D", "," )  --> { "A/B", "C/D" }<br>
 * </p>
 *
 * @param text The string that should be splitted into substrings
 * @param delimiters All characters that should be recognized as a
 *        separator or substrings
 *
 * @return An array of substrings of the given text
 *
 * @see #parts(String, String)
 * @see #substrings(String, String)
 * @see #allSubstrings(String, String)
 */
    public String[] allParts(String text, String delimiters) {
        return this.parts(text, delimiters, true);
    }

    // allParts()
    // -------------------------------------------------------------------------

    /**
 * Returns the given text split up into an array of strings, at the
 * occurrances of the separator string.  In contrary to method parts() the
 * separator is a one or many character sequence delimiter. That is, only
 * the exact sequence  of the characters in separator identifies the end
 * of a substring. Subsequent occurences of separator will be skipped.
 * Therefore no empty strings ("") will be in the result array.
 *
 * @param text The text to be split up
 * @param separator The string that separates the substrings
 *
 * @return An array of substrings not containing any separator anymore
 *
 * @see #allSubstrings(String, String)
 * @see #parts(String, String)
 * @see #allParts(String, String)
 */
    public String[] substrings(String text, String separator) {
        return this.substrings(text, separator, false);
    }

    // substrings()
    // -------------------------------------------------------------------------

    /**
 * Returns the given text split up into an array of strings, at the
 * occurrances of the separator string.  In contrary to method allParts()
 * the separator is a one or many character sequence delimiter. That is,
 * only the exact sequence  of the characters in separator identifies the
 * end of a substring. Subsequent occurences of separator are not skipped.
 * They are added as empty strings to the result.
 *
 * @param text The text to be split up
 * @param separator The string that separates the substrings
 *
 * @return An array of substrings not containing any separator anymore
 *
 * @see #substrings(String, String)
 * @see #parts(String, String)
 * @see #allParts(String, String)
 */
    public String[] allSubstrings(String text, String separator) {
        return this.substrings(text, separator, true);
    }

    // allSubstrings()
    // -------------------------------------------------------------------------

    /**
 * Returns the first substring that is enclosed by the specified
 * delimiters.   <br>
 * The delimiters are not included in the return string.
 *
 * <p>
 * Example:<br> getDelimitedSubstring( "This {placeholder} belongs to me",
 * "{", "}" ) --> returns "placeholder"
 * </p>
 *
 * @param text The input string that contains the delimited part
 * @param startDelimiter The start delimiter of the substring
 * @param endDelimiter The end delimiter of the substring
 *
 * @return The substring or an empty string, if no delimiters are found.
 */
    public String getDelimitedSubstring(String text, String startDelimiter,
        String endDelimiter) {
        int start;
        int stop;
        String subStr = "";

        if ((text != null) && (startDelimiter != null) &&
                (endDelimiter != null)) {
            start = text.indexOf(startDelimiter);

            if (start >= 0) {
                stop = text.indexOf(endDelimiter, start + 1);

                if (stop > start) {
                    subStr = text.substring(start + 1, stop);
                }
            }
        }

        return subStr;
    }

    // getDelimitedSubstring()
    // -------------------------------------------------------------------------

    /**
     * Returns the first substring that is enclosed by the specified delimiter. <br>
 * The delimiters are not included in the return string.
 *
 * <p>
 * Example:<br> getDelimitedSubstring( "File 'text.txt' not found.", "'",
 * "'" ) --> returns "text.txt"
 * </p>
 *
 * @param text The input string that contains the delimited part
 * @param delimiter The start and end delimiter of the substring
 *
 * @return The substring or an empty string, if no delimiters are found.
 */
    public String getDelimitedSubstring(String text, String delimiter) {
        return this.getDelimitedSubstring(text, delimiter, delimiter);
    }

    // getDelimitedSubstring()
    // -------------------------------------------------------------------------

    /**
 * Prints the stack trace of the specified throwable to a string and
 * returns it.
 *
 * @param throwable
 *
 * @return
 */
    public String stackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();

        return sw.toString();
    }

    // stackTrace()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string filled (on the left) up to the specified length
 * with the given character.     <br>
 * Example: leftPadCh( "12", 6, '0' ) --> "000012"
 *
 * @param str
 * @param len
 * @param ch
 *
 * @return
 */
    public String leftPadCh(String str, int len, char ch) {
        return this.padCh(str, len, ch, true);
    }

    // leftPadCh()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string filled (on the left) up to the specified length
 * with spaces.     <br>
 * Example: leftPad( "XX", 4 ) --> "  XX"
 *
 * @param str The string that has to be filled up to the specified length
 * @param len The length of the result string
 *
 * @return
 */
    public String leftPad(String str, int len) {
        return this.leftPadCh(str, len, CH_SPACE);
    }

    // leftPad()
    // -------------------------------------------------------------------------

    /**
 * Returns the given integer as string filled (on the left) up to the
 * specified length with the given fill character.     <br>
 * Example: leftPad( 24, 5, '' ) --> "24"
 *
 * @param value
 * @param len
 * @param fillChar
 *
 * @return
 */
    public String leftPadCh(int value, int len, char fillChar) {
        return this.leftPadCh(Integer.toString(value), len, fillChar);
    }

    // leftPadCh()
    // -------------------------------------------------------------------------

    /**
 * Returns the given integer as string filled (on the left) up to the
 * specified length with zeroes.     <br>
 * Example: leftPad( 12, 4 ) --> "0012"
 *
 * @param value
 * @param len
 *
 * @return
 */
    public String leftPad(int value, int len) {
        return this.leftPadCh(value, len, '0');
    }

    // leftPad()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string filled (on the right) up to the specified
 * length with the given character.     <br>
 * Example: rightPadCh( "34", 5, 'X' ) --> "34XXX"
 *
 * @param str
 * @param len
 * @param ch
 *
 * @return
 */
    public String rightPadCh(String str, int len, char ch) {
        return this.padCh(str, len, ch, false);
    }

    // rightPadCh()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string filled (on the right) up to the specified
 * length with spaces.     <br>
 * Example: rightPad( "88", 6 ) --> "88    "
 *
 * @param str
 * @param len
 *
 * @return
 */
    public String rightPad(String str, int len) {
        return this.rightPadCh(str, len, CH_SPACE);
    }

    // rightPad()
    // -------------------------------------------------------------------------

    /**
 * Returns the given integer as string filled (on the right) up to the
 * specified length with the given character.     <br>
 * Example: rightPad( "32", 4, '#' ) --> "32##"
 *
 * @param value
 * @param len
 * @param fillChar
 *
 * @return
 */
    public String rightPadCh(int value, int len, char fillChar) {
        return this.rightPadCh(Integer.toString(value), len, fillChar);
    }

    // rightPad()
    // -------------------------------------------------------------------------

    /**
 * Returns the given integer as string filled (on the right) up to the
 * specified length with spaces.     <br>
 * Example: rightPad( "17", 5 ) --> "17   "
 *
 * @param value
 * @param len
 *
 * @return
 */
    public String rightPad(int value, int len) {
        return this.rightPadCh(value, len, CH_SPACE);
    }

    // rightPad()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string filled equally left and right up to the
 * specified length with the given character.     <br>
 * Example: centerCh( "A", 5, '_' ) --> "__A__"  <br>
 * Example: centerCh( "XX", 7, '+' ) --> "++XX+++"
 *
 * @param str
 * @param len
 * @param ch
 *
 * @return
 */
    public String centerCh(String str, int len, char ch) {
        String buffer = null;
        int missing = len - str.length();
        int half = 0;

        if (missing <= 0) {
            return str;
        }

        half = missing / 2;
        buffer = this.rightPadCh(str, len - half, ch);

        return this.leftPadCh(buffer, len, ch);
    }

    // centerCh()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string filled (on the right and right) up to the
 * specified length with spaces.     <br>
 * Example: center( "Mike", 10 ) --> "   Mike   "
 *
 * @param str
 * @param len
 *
 * @return
 */
    public String center(String str, int len) {
        return this.centerCh(str, len, CH_SPACE);
    }

    // center()
    // -------------------------------------------------------------------------

    /**
 * Returns the given string array extended by one element that hold the
 * specified string.
 *
 * @param strings
 * @param string
 *
 * @return
 */
    public String[] append(String[] strings, String string) {
        String[] appStr = { string };

        return this.append(strings, appStr);
    }

    // append()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of strings that contains all strings given by the first
 * and second string array. The strings from the  second array will be
 * added at the end of the first array.
 *
 * @param strings The array of string to which to append
 * @param appendStrings The string to be appended to the first array
 *
 * @return
 */
    public String[] append(String[] strings, String[] appendStrings) {
        String[] newStrings = null;

        if (strings == null) {
            return appendStrings;
        }

        if (appendStrings == null) {
            return strings;
        }

        newStrings = new String[strings.length + appendStrings.length];
        System.arraycopy(strings, 0, newStrings, 0, strings.length);
        System.arraycopy(appendStrings, 0, newStrings, strings.length,
            appendStrings.length);

        return newStrings;
    }

    // append()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of strings that contains all strings given in the first
 * plus the specified string to append, if it is not already in the given
 * array.
 *
 * @param strings
 * @param appendString
 *
 * @return
 */
    public String[] appendIfNotThere(String[] strings, String appendString) {
        if (this.contains(strings, appendString)) {
            return strings;
        } else {
            return this.append(strings, appendString);
        }
    }

    // appendIfNotThere()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of strings that contains all strings given in the first
 * plus all strings of the second array that are not already in the first
 * array.
 *
 * @param strings
 * @param appendStrings
 *
 * @return
 */
    public String[] appendIfNotThere(String[] strings, String[] appendStrings) {
        String[] newStrings = strings;

        if (appendStrings == null) {
            return newStrings;
        }

        for (int i = 0; i < appendStrings.length; i++) {
            newStrings = this.appendIfNotThere(newStrings, appendStrings[i]);
        }

        return newStrings;
    }

    // appendIfNotThere()
    // -------------------------------------------------------------------------

    /**
 * Removes all string of the second array from the first array. Returns a
 * new array of string that contains all remaining strings of the original
 * strings array.
 *
 * @param strings The array from which to remove the strings
 * @param removeStrings The strings to be removed
 *
 * @return
 */
    public String[] remove(String[] strings, String[] removeStrings) {
        if ((strings == null) || (removeStrings == null) ||
                (strings.length == 0) || (removeStrings.length == 0)) {
            return strings;
        }

        return this.removeFromStringArray(strings, removeStrings);
    }

    // remove()
    // -------------------------------------------------------------------------

    /**
 * Removes the given string from the specified string array. Returns a new
 * array of string that contains all remaining strings of the original
 * strings array.
 *
 * @param strings The array from which to remove the string
 * @param removeString The string to be removed
 *
 * @return
 */
    public String[] remove(String[] strings, String removeString) {
        String[] removeStrings = { removeString };

        return this.remove(strings, removeStrings);
    }

    // remove()
    // -------------------------------------------------------------------------

    /**
 * Removes all null values from the given string array. Returns a new
 * string array that contains all none null values of the  input array.
 *
 * @param strings The array to be cleared of null values
 *
 * @return
 */
    public String[] removeNull(String[] strings) {
        if (strings == null) {
            return strings;
        }

        return this.removeFromStringArray(strings, null);
    }

    // removeNull()
    // -------------------------------------------------------------------------

    /**
 * Returns a string that contains all given strings concatenated and
 * separated by the specified separator.
 *
 * @param strings The array of strings that should be concatenated
 * @param separator The separator between the strings
 *
 * @return One string containing the concatenated strings separated by
 *         separator
 */
    public String asString(String[] strings, String separator) {
        StringBuffer buffer = null;
        buffer = new StringBuffer(strings.length * 20);

        if (strings.length > 0) {
            buffer.append(strings[0].toString());

            for (int i = 1; i < strings.length; i++) {
                buffer.append(separator);

                if (strings[i] != null) {
                    buffer.append(strings[i]);
                }
            }
        }

        return buffer.toString();
    }

    // asString()
    // -------------------------------------------------------------------------

    /**
 * Returns a string that contains all given strings concatenated and
 * separated by comma.
 *
 * @param strings The array of strings that should be concatenated
 *
 * @return One string containing the concatenated strings separated by
 *         comma (",")
 */
    public String asString(String[] strings) {
        return this.asString(strings, ",");
    }

    // asString()
    // -------------------------------------------------------------------------

    /**
 * Returns the index of the first string in the given string array that
 * matches the specified string pattern. If no string is found in the
 * array the result is -1.
 *
 * @param strArray An array of string (may contain null elements)
 * @param pattern The pattern the searched string must match
 *
 * @return The index of the matching string in the array or -1 if not found
 */
    public int indexOf(String[] strArray, StringPattern pattern) {
        if ((strArray == null) || (strArray.length == 0)) {
            return -1;
        }

        boolean found = false;

        for (int i = 0; i < strArray.length; i++) {
            if (strArray[i] == null) {
                if (pattern == null) {
                    found = true;
                }
            } else {
                if (pattern != null) {
                    found = pattern.matches(strArray[i]);
                }
            }

            if (found) {
                return i;
            }
        }

        return -1;
    }

    // indexOf()
    // -------------------------------------------------------------------------

    /**
 * Returns the index of the specified string in the given string array. It
 * returns the index of the first occurrence of the string. If the string
 * is not found in the array the result is -1. The comparison of the
 * strings is case-sensitive!
 *
 * @param strArray An array of string (may contain null elements)
 * @param searchStr The string to be looked up in the array (null allowed)
 *
 * @return The index of the string in the array or -1 if not found
 */
    public int indexOf(String[] strArray, String searchStr) {
        return this.indexOfString(strArray, searchStr, false);
    }

    // indexOf()
    // -------------------------------------------------------------------------

    /**
 * Returns the index of the specified string in the given string array. It
 * returns the index of the first occurrence of the string. If the string
 * is not found in the array the result is -1. The comparison of the
 * strings is case-insensitive!
 *
 * @param strArray An array of string (may contain null elements)
 * @param searchStr The string to be looked up in the array (null allowed)
 *
 * @return The index of the string in the array or -1 if not found
 */
    public int indexOfIgnoreCase(String[] strArray, String searchStr) {
        return this.indexOfString(strArray, searchStr, true);
    }

    // indexOfIgnoreCase()
    // -------------------------------------------------------------------------

    /**
 * Returns whether or not the specified string can be found in the given
 * string array.
 *
 * @param strArray An array of string (may contain null elements)
 * @param searchStr The string to be looked up in the array (null allowed)
 * @param ignoreCase Defines whether or not the comparison is
 *        case-sensitive.
 *
 * @return true, if the specified array contains the given string
 */
    public boolean contains(String[] strArray, String searchStr,
        boolean ignoreCase) {
        if (ignoreCase) {
            return this.containsIgnoreCase(strArray, searchStr);
        } else {
            return this.contains(strArray, searchStr);
        }
    }

    // contains()
    // -------------------------------------------------------------------------

    /**
 * Returns whether or not a string can be found in the given string array
 * that matches the specified string pattern.
 *
 * @param strArray An array of string (may contain null elements)
 * @param pattern The string pattern to match against in the array (null
 *        allowed)
 *
 * @return true, if the specified array contains a string matching the
 *         pattern
 */
    public boolean contains(String[] strArray, StringPattern pattern) {
        return (this.indexOf(strArray, pattern) >= 0);
    }

    // contains()
    // -------------------------------------------------------------------------

    /**
 * Returns whether or not the specified string can be found in the given
 * string array. The comparison of the strings is case-sensitive!
 *
 * @param strArray An array of string (may contain null elements)
 * @param searchStr The string to be looked up in the array (null allowed)
 *
 * @return true, if the specified array contains the given string
 */
    public boolean contains(String[] strArray, String searchStr) {
        return (this.indexOf(strArray, searchStr) >= 0);
    }

    // contains()
    // -------------------------------------------------------------------------

    /**
 * Returns whether or not the specified string can be found in the given
 * string array. The comparison of the strings is case-insensitive!
 *
 * @param strArray An array of string (may contain null elements)
 * @param searchStr The string to be looked up in the array (null allowed)
 *
 * @return true, if the specified array contains the given string
 */
    public boolean containsIgnoreCase(String[] strArray, String searchStr) {
        return (this.indexOfIgnoreCase(strArray, searchStr) >= 0);
    }

    // containsIgnoreCase()
    // -------------------------------------------------------------------------

    /**
 * Returns all elements of string array <i>from</i> in a new array from
 * index start up to the end. If start index is larger than the array's
 * length, an empty array will be returned.
 *
 * @param from The string array the elements should be copied from
 * @param start Index of the first element to copy
 *
 * @return
 */
    public String[] copyFrom(String[] from, int start) {
        if (from == null) {
            return null;
        }

        return this.copyFrom(from, start, from.length - 1);
    }

    // copyFrom()
    // -------------------------------------------------------------------------

    /**
 * Returns all elements of string array <i>from</i> in a new array from
 * index start up to index end (inclusive). If end is larger than the last
 * valid index, it will be reduced to the last index. If end index is less
 * than start index, an empty array will be returned.
 *
 * @param from The string array the elements should be copied from
 * @param start Index of the first element to copy
 * @param end Index of last element to be copied
 *
 * @return
 */
    public String[] copyFrom(String[] from, int start, int end) {
        String[] result;
        int count;
        int stop = end;

        if (from == null) {
            return null;
        }

        if (stop > (from.length - 1)) {
            stop = from.length - 1;
        }

        count = stop - start + 1;

        if (count < 1) {
            return new String[0];
        }

        result = new String[count];
        System.arraycopy(from, start, result, 0, count);

        return result;
    }

    // copyFrom()
    // -------------------------------------------------------------------------

    /**
 * Returns the portion of the given string that comes before the last
 * occurance of the specified separator.    <br>
 * If the separator could not be found in the given string, then the
 * string is returned unchanged.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * cutTail( "A/B/C", "/" ) ;   // returns "A/B" <br>
 * cutTail( "A/B/C", "," ) ;   // returns "A/B/C"
 * </p>
 *
 * <p></p>
 *
 * @param text The string from which to cut off the tail
 * @param separator The separator from where to cut off
 *
 * @return the string without the separator and without the characters
 *         after the separator
 *
 * @see #prefix( String, String )
 * @see #suffix( String, String )
 * @see #cutHead( String, String )
 * @see #startingFrom( String, String )
 * @see #upTo( String, String )
 */
    public String cutTail(String text, String separator) {
        int index;

        if ((text == null) || (separator == null)) {
            return text;
        }

        index = text.lastIndexOf(separator);

        if (index < 0) {
            return text;
        }

        return text.substring(0, index);
    }

    // cutTail()
    // ------------------------------------------------------------------------

    /**
 * Returns the portion of the given string that stands after the last
 * occurance of the specified separator.    <br>
 * If the separator could not be found in the given string, then the
 * string is returned unchanged.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * cutHead( "A/B/C", "/" ) ;   // returns "C" <br>
 * cutHead( "A/B/C", "," ) ;   // returns "A/B/C"
 * </p>
 *
 * <p></p>
 *
 * @param text The string from which to cut off the head
 * @param separator The separator up to which to cut off
 *
 * @return the string without the separator and without the characters
 *         before the separator
 *
 * @see #prefix( String, String )
 * @see #cutTail( String, String )
 * @see #suffix( String, String )
 * @see #startingFrom( String, String )
 * @see #upTo( String, String )
 */
    public String cutHead(String text, String separator) {
        int index;

        if ((text == null) || (separator == null)) {
            return text;
        }

        index = text.lastIndexOf(separator);

        if (index < 0) {
            return text;
        }

        return text.substring(index + 1);
    }

    // cutHead()
    // ------------------------------------------------------------------------

    /**
 * Returns a string array with two elements where the first is the
 * attribute name and the second is the attribute value. Splits the given
 * string at the first occurance of separator and returns the piece before
 * the separator in element 0 and the piece after the  separator in the
 * returned array. If the separator is not found, the first element
 * contains the full string and the second an empty string.
 *
 * @param str The string that contains the name-value pair
 * @param separator The separator between name and value
 *
 * @return
 */
    public String[] splitNameValue(String str, String separator) {
        String[] result = { "", "" };
        int index;

        if (str != null) {
            index = str.indexOf(separator);

            if (index > 0) {
                result[0] = str.substring(0, index);
                result[1] = str.substring(index + separator.length());
            } else {
                result[0] = str;
            }
        }

        return result;
    }

    // splitNameValue()
    // -------------------------------------------------------------------------

    /**
 * Returns the substring of the given string that comes before the first
 * occurance of the specified separator. If the string starts with a
 * separator, the result will be an empty string. If the string doesn't
 * contain the separator the method returns null.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * prefix( "A/B/C", "/" ) ;   // returns "A" <br>
 * prefix( "A/B/C", "," ) ;   // returns null
 * </p>
 *
 * <p></p>
 *
 * @param str The string of which the prefix is desired
 * @param separator Separates the prefix from the rest of the string
 *
 * @return
 *
 * @see #suffix( String, String )
 * @see #cutTail( String, String )
 * @see #cutHead( String, String )
 * @see #startingFrom( String, String )
 * @see #upTo( String, String )
 */
    public String prefix(String str, String separator) {
        return this.prefix(str, separator, true);
    }

    // prefix()
    // -------------------------------------------------------------------------

    /**
 * Returns the substring of the given string that comes after the first
 * occurance of the specified separator. If the string ends with a
 * separator, the result will be an empty string. If the string doesn't
 * contain the separator the method returns null.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * suffix( "A/B/C", "/" ) ;   // returns "B/C" <br>
 * suffix( "A/B/C", "," ) ;   // returns null
 * </p>
 *
 * <p></p>
 *
 * @param str The string of which the suffix is desired
 * @param separator Separates the suffix from the rest of the string
 *
 * @return
 *
 * @see #prefix( String, String )
 * @see #cutTail( String, String )
 * @see #cutHead( String, String )
 * @see #startingFrom( String, String )
 * @see #upTo( String, String )
 */
    public String suffix(String str, String separator) {
        return this.suffix(str, separator, true);
    }

    // suffix()
    // -------------------------------------------------------------------------

    /**
 * Returns the substring of the given string that comes before the first
 * occurance of the specified separator. If the string starts with a
 * separator, the result will be an empty string. If the string doesn't
 * contain the separator the method returns the whole string unchanged.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * upTo( "A/B/C", "/" ) ;   // returns "A" <br>
 * upTo( "A/B/C", "," ) ;   // returns "A/B/C" <br>
 * upTo( "/A/B/C", "/" ) ;   // returns ""
 * </p>
 *
 * <p></p>
 *
 * @param str The string of which the prefix is desired
 * @param separator Separates the prefix from the rest of the string
 *
 * @return
 *
 * @see #prefix( String, String )
 * @see #cutTail( String, String )
 * @see #cutHead( String, String )
 * @see #startingFrom( String, String )
 * @see #suffix( String, String )
 */
    public String upTo(String str, String separator) {
        return this.prefix(str, separator, false);
    }

    // upTo()
    // -------------------------------------------------------------------------

    /**
 * Returns the substring of the given string that comes after the first
 * occurance of the specified separator. If the string doesn't contain the
 * separator the method returns the whole string unchanged.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <p>
 * startingFrom( "A/B/C", "/" ) ;   // returns "B/C" <br>
 * startingFrom( "A/B/C", "," ) ;   // returns "A/B/C"
 * </p>
 *
 * <p></p>
 *
 * @param str The string of which the suffix is desired
 * @param separator Separates the suffix from the rest of the string
 *
 * @return
 *
 * @see #prefix( String, String )
 * @see #cutTail( String, String )
 * @see #cutHead( String, String )
 * @see #suffix( String, String )
 * @see #upTo( String, String )
 */
    public String startingFrom(String str, String separator) {
        return this.suffix(str, separator, false);
    }

    // startingFrom()
    // -------------------------------------------------------------------------

    /**
 * Returns a string that contains all characters of the given string in
 * reverse order.
 *
 * @param str
 *
 * @return
 */
    public String reverse(String str) {
        if (str == null) {
            return null;
        }

        char[] newStr = new char[str.length()];
        StringCharacterIterator iterator = new StringCharacterIterator(str);
        int i = 0;

        for (char ch = iterator.last(); ch != CharacterIterator.DONE;
                ch = iterator.previous()) {
            newStr[i] = ch;
            i++;
        }

        return new String(newStr);
    }

    // reverse()
    // -------------------------------------------------------------------------

    /**
 * Returns the given map with new entries from the specified String. If the
     * specified map is null a new empty java.util.Hashtable will be  created. <br>
 * The string is split up into elements separated by the elementSeparator
     * parameter. If this parameter is null the default separator "," is used. <br>
 * After that each part is split up to a key-value pair separated by the
 * keyValueSeparator parameter. If this parameter is null the default "="
 * is used. <br>
 * Then the key-value pairs are added to the map and the map is returned.
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 * @param elementSeparator The separator between the elements of the list
 * @param keyValueSeparator The separator between the keys and values
 * @param map The map to which the key-value pairs are added
 *
 * @return
 */
    public Map toMap(String str, String elementSeparator,
        String keyValueSeparator, Map map) {
        Map result;
        String elemSep;
        String kvSep;
        String[] assignments;
        String[] nameValue;

        if (str == null) {
            return map;
        }

        result = ((map == null) ? new Hashtable() : map);
        elemSep = (elementSeparator == null) ? "," : elementSeparator;
        kvSep = (keyValueSeparator == null) ? "=" : keyValueSeparator;
        assignments = this.parts(str, elemSep);

        for (int i = 0; i < assignments.length; i++) {
            nameValue = this.splitNameValue(assignments[i], kvSep);
            nameValue[0] = nameValue[0].trim();
            nameValue[1] = nameValue[1].trim();

            if (nameValue[0].length() > 0) {
                result.put(nameValue[0], nameValue[1]);
            }
        }

        return result;
    }

    // asMap()
    // -------------------------------------------------------------------------

    /**
 * Returns a new map object that contains all key-value pairs of the
 * specified string.  <br>
 * The separator between the elements is assumed to be "," and "=" between
 * key and value.
 *
 * <p>
 * Example:<br> "main=Fred,support1=John,support2=Stella,manager=Oscar"
 * </p>
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string with the list of key-value pairs
 *
 * @return
 */
    public Map asMap(String str) {
        return this.toMap(str, null, null, null);
    }

    // asMap()
    // -------------------------------------------------------------------------

    /**
 * Returns a new map object that contains all key-value pairs of the
 * specified string.  <br>
 * The separator between the keys and values is assumed to be "=".
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 * @param elementSeparator The separator between the elements of the list
 *
 * @return
 */
    public Map asMap(String str, String elementSeparator) {
        return this.toMap(str, elementSeparator, null, null);
    }

    // asMap()
    // -------------------------------------------------------------------------

    /**
 * Returns a new map object that contains all key-value pairs of the
 * specified string.
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 * @param elementSeparator The separator between the elements of the list
 * @param keyValueSeparator The separator between the keys and values
 *
 * @return
 */
    public Map asMap(String str, String elementSeparator,
        String keyValueSeparator) {
        return this.toMap(str, elementSeparator, keyValueSeparator, null);
    }

    // asMap()
    // -------------------------------------------------------------------------

    /**
 * Returns the given map object with all key-value pairs of the specified
 * string added to it. <br>
 * The separator between the keys and values is assumed to be "=".
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 * @param elementSeparator The separator between the elements of the list
 * @param map The map to which the key-value pairs are added
 *
 * @return
 */
    public Map toMap(String str, String elementSeparator, Map map) {
        return this.toMap(str, elementSeparator, null, map);
    }

    // toMap()
    // -------------------------------------------------------------------------

    /**
 * Adds all key-value pairs of the given string to the specified map. <br>
 * The separator between the elements is assumed to be "," and "=" between
 * key and value.
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 * @param map The map to which the key-value pairs are added
 *
 * @return
 */
    public Map toMap(String str, Map map) {
        return this.toMap(str, null, null, map);
    }

    // toMap()
    // -------------------------------------------------------------------------

    /**
     * Adds all key-value pairs of the given string to a new properties object. <br>
 * The separator between the elements is assumed to be "," and "=" between
 * key and value.
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 *
 * @return
 */
    public Properties asProperties(String str) {
        return this.toProperties(str, null);
    }

    // asProperties()
    // -------------------------------------------------------------------------

    /**
 * Adds all key-value pairs of the given string to the specified
 * properties. <br>
 * The separator between the elements is assumed to be "," and "=" between
 * key and value.
 *
 * <p>
 * <b>Be aware that all leading and trailing whitespaces of keys and values
 * will be removed!</b>
 * </p>
 *
 * @param str The string that contains the list of key-value pairs
 * @param properties The properties where the key-value pairs should be
 *        added
 *
 * @return
 */
    public Properties toProperties(String str, Properties properties) {
        Properties props = (properties == null) ? new Properties() : properties;

        return (Properties) this.toMap(str, null, null, props);
    }

    // toProperties()
    // -------------------------------------------------------------------------
    // =========================================================================
    // PROTECTED INSTANCE METHODS
    // =========================================================================

    /**
 * Cuts off all leading and trailing occurences of separator in text.
 *
 * @param text
 * @param separator
 *
 * @return
 */
    protected String trimSeparator(String text, String separator) {
        int sepLen = separator.length();

        while (text.startsWith(separator)) {
            text = text.substring(separator.length());
        }

        while (text.endsWith(separator)) {
            text = text.substring(0, text.length() - sepLen);
        }

        return text;
    }

    // trimSeparator()
    // -------------------------------------------------------------------------

    /**
 * Returns an array of substrings of the given text.    <br>
 * The separators between the substrings are the given delimiters. Each
 * character in the delimiter string is treated as a separator.
 *
 * @param text The string that should be splitted into substrings
 * @param delimiters All characters that should be recognized as a
 *        separator or substrings
 * @param all If true, empty elements will be returned, otherwise thye are
 *        skipped
 *
 * @return An array of substrings of the given text
 */
    protected String[] parts(String text, String delimiters, boolean all) {
        ArrayList result = null;
        StringTokenizer tokenizer = null;

        if (text == null) {
            return null;
        }

        if ((delimiters == null) || (delimiters.length() == 0)) {
            String[] resultArray = { text };

            return resultArray;
        }

        if (text.length() == 0) {
            return new String[0];
        } else {
            result = new ArrayList();
            tokenizer = new StringTokenizer(text, delimiters, all);

            if (all) {
                this.collectParts(result, tokenizer, delimiters);
            } else {
                this.collectParts(result, tokenizer);
            }
        }

        return (String[]) result.toArray(new String[0]);
    }

    // parts()
    // -------------------------------------------------------------------------
    protected void collectParts(List list, StringTokenizer tokenizer) {
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
    }

    // collectParts()
    // -------------------------------------------------------------------------
    protected void collectParts(List list, StringTokenizer tokenizer,
        String delimiter) {
        String token;
        boolean lastWasDelimiter = false;

        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();

            if (delimiter.indexOf(token) >= 0) {
                if (lastWasDelimiter) {
                    list.add("");
                }

                lastWasDelimiter = true;
            } else {
                list.add(token);
                lastWasDelimiter = false;
            }
        }
    }

    // collectParts()
    // -------------------------------------------------------------------------

    /**
 * Returns the given text split up into an array of strings, at the
 * occurrances of the separator string.  In contrary to method parts() the
 * separator is a one or many character sequence delimiter. That is, only
 * the exact sequence  of the characters in separator identifies the end
 * of a substring. Parameter all defines whether empty strings between
 * consecutive separators are added to the result or not.
 *
 * @param text The text to be split up
 * @param separator The string that separates the substrings
 * @param all If true, empty strings are added, otherwise skipped
 *
 * @return An array of substrings not containing any separator anymore
 *
 * @see #parts(String, String, boolean)
 */
    protected String[] substrings(String text, String separator, boolean all) {
        int index = 0;
        int start = 0;
        int sepLen = 0;
        int strLen = 0;
        String str = text;
        ArrayList strings = new ArrayList();

        if (text == null) {
            return new String[0];
        }

        if ((separator == null) || (separator.length() == 0)) {
            if (text.length() == 0) {
                return new String[0];
            }

            String[] resultArray = { text };

            return resultArray;
        }

        if (!all) {
            str = this.trimSeparator(text, separator);
        }

        strLen = str.length();

        if (strLen > 0) {
            sepLen = separator.length();
            index = str.indexOf(separator, start);

            while (index >= 0) {
                if (all) {
                    if (index > 0) {
                        strings.add(str.substring(start, index));
                    }
                } else {
                    if (index > (start + sepLen)) {
                        strings.add(str.substring(start, index));
                    }
                }

                start = index + sepLen;
                index = str.indexOf(separator, start);
            }

            if (start < strLen) {
                strings.add(str.substring(start));
            }
        }

        return (String[]) strings.toArray(new String[0]);
    }

    // substrings()
    // -------------------------------------------------------------------------
    protected String padCh(String str, int len, char ch, boolean left) {
        StringBuffer buffer = null;
        int missing = len - str.length();

        if (missing <= 0) {
            return str;
        }

        buffer = new StringBuffer(len);

        if (!left) {
            buffer.append(str);
        }

        for (int i = 1; i <= missing; i++) {
            buffer.append(ch);
        }

        if (left) {
            buffer.append(str);
        }

        return buffer.toString();
    }

    // padCh()
    // -------------------------------------------------------------------------
    protected int indexOfString(String[] strArray, String searchStr,
        boolean ignoreCase) {
        if ((strArray == null) || (strArray.length == 0)) {
            return -1;
        }

        boolean found = false;

        for (int i = 0; i < strArray.length; i++) {
            if (strArray[i] == null) {
                if (searchStr == null) {
                    found = true;
                }
            } else {
                if (ignoreCase) {
                    found = strArray[i].equalsIgnoreCase(searchStr);
                } else {
                    found = strArray[i].equals(searchStr);
                }
            }

            if (found) {
                return i;
            }
        }

        return -1;
    }

    // indexOfString()
    // -------------------------------------------------------------------------

    /**
 * Returns the substring of the given string that comes before the first
 * occurance of the specified separator. If the string starts with a
 * separator, the result will be an empty string. If the string doesn't
 * contain the separator the method returns null or the whole string,
 * depending on the returnNull flag.
 *
 * @param str The string of which the prefix is desired
 * @param separator Separates the prefix from the rest of the string
 * @param returnNull Specifies if null will be returned if no separator is
 *        found
 *
 * @return
 */
    protected String prefix(String str, String separator, boolean returnNull) {
        if (str == null) {
            return null;
        }

        if (separator == null) {
            return (returnNull ? null : str);
        }

        int index = str.indexOf(separator);

        if (index >= 0) {
            return str.substring(0, index);
        } else {
            return (returnNull ? null : str);
        }
    }

    // prefix()
    // -------------------------------------------------------------------------

    /**
 * Returns the substring of the given string that comes after the first
 * occurance of the specified separator. If the string ends with a
 * separator, the result will be an empty string. If the string doesn't
 * contain the separator the method returns null or the whole string,
 * depending on the returnNull flag.
 *
 * @param str The string of which the suffix is desired
 * @param separator Separates the suffix from the rest of the string
 * @param returnNull Specifies if null will be returned if no separator is
 *        found
 *
 * @return
 */
    protected String suffix(String str, String separator, boolean returnNull) {
        if (str == null) {
            return null;
        }

        if (separator == null) {
            return (returnNull ? null : str);
        }

        int index = str.indexOf(separator);

        if (index >= 0) {
            return str.substring(index + separator.length());
        } else {
            return (returnNull ? null : str);
        }
    }

    // suffix()
    // -------------------------------------------------------------------------

    /**
 * Removes the given strings from the array. If removeStrings is null it
 * means that all null values are removed from the first array.
 *
 * @param strings
 * @param removeStrings
 *
 * @return
 */
    protected String[] removeFromStringArray(String[] strings,
        String[] removeStrings) {
        List list;
        boolean remains;
        list = new ArrayList(strings.length);

        for (int i = 0; i < strings.length; i++) {
            if (removeStrings == null) {
                remains = strings[i] != null;
            } else {
                remains = !this.contains(removeStrings, strings[i]);
            }

            if (remains) {
                list.add(strings[i]);
            }
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    // removeFromStringArray()
    // -------------------------------------------------------------------------
}


// class StringUtil
