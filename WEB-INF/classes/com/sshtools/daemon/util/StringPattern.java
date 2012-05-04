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
// CONTENT  : CLASS StringPattern
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.7 - 13/02/2003
// HISTORY  :
//  24/01/2000  duma  CREATED
//  08/01/2002  duma  bugfix  -> Handle *xxx (equal characters after star) correctly
//  16/01/2002  duma  changed -> Implements Serializable
//	06/07/2002	duma	bugfix	-> Couldn't match "London" on "L*n"
//	19/09/2002	duma	bugfix	-> Couldn't match "MA_DR_HRBLUB" on "*_HR*"
//	19/09/2002	duma	changed	-> Using now StringExaminer instead of CharacterIterator
//	29/09/2002	duma	changed	-> Refactored: Using StringExaminer instead of StringScanner
//	26/12/2002	duma	changed	-> Comment of matches() was wrong / new hasWildcard()
//	13/02/2003	duma	added		-> setDigitWildcardChar()
//
// Copyright (c) 2000-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package com.sshtools.daemon.util;


// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.*;


/**
 * This class provides services for checking strings against string-patterns.
 * Currently it supports the wildcards<br>
 * '' for any number of any character and <br>
 * '?' for any one character. The API is very simple:<br>
 * <br>
 * There are only the two class methods <i>match()</i> and
 * <i>matchIgnoreCase()</i>. <br>
 * Example: <br>
 * StringPattern.match( 'Hello World", "H W" ) ;  --> evaluates to true  <br>
 * StringPattern.matchIgnoreCase( 'StringPattern", "str???pat" ) ;  -->
 * evaluates to true  <br>
 *
 * @author Manfred Duchrow
 * @version 1.7
 */
public class StringPattern implements Serializable {
    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /**  */
    protected final static String MULTI_WILDCARD = "*";

    /**  */
    protected final static char MULTICHAR_WILDCARD = '*';

    /**  */
    protected final static char SINGLECHAR_WILDCARD = '?';

    // =========================================================================
    // INSTANCE VARIABLES
    // =========================================================================
    private boolean ignoreCase = false;
    private String pattern = null;

    // -------------------------------------------------------------------------
    private Character digitWildcard = null;

    // -------------------------------------------------------------------------
    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /**
 * Initializes the new instance with the string pattern and the selecteion,
 * if case should be ignored when comparing characters.
 *
 * @param pattern The pattern to check against ( May contain '' and '?'
 *        wildcards )
 * @param ignoreCase Definition, if case sensitive character comparison or
 *        not.
 */
    public StringPattern(String pattern, boolean ignoreCase) {
        this.setPattern(pattern);
        this.setIgnoreCase(ignoreCase);
    }

    // StringPattern()
    // -------------------------------------------------------------------------

    /**
 * Initializes the new instance with the string pattern. The default is
 * case sensitive checking.
 *
 * @param pattern The pattern to check against ( May contain '' and '?'
 *        wildcards )
 */
    public StringPattern(String pattern) {
        this(pattern, false);
    }

    // StringPattern()
    // -------------------------------------------------------------------------

    /**
 * Initializes the new instance with the string pattern and a digit
 * wildcard  character. The default is case sensitive checking.
 *
 * @param pattern The pattern to check against ( May contain '', '?'
 *        wildcards and the digit wildcard )
 * @param digitWildcard A wildcard character that stands as placeholder for
 *        digits
 */
    public StringPattern(String pattern, char digitWildcard) {
        this(pattern, false, digitWildcard);
    }

    // StringPattern()
    // -------------------------------------------------------------------------

    /**
 * Initializes the new instance with the string pattern and the selecteion,
 * if case should be ignored when comparing characters plus a wildcard
 * character for digits.
 *
 * @param pattern The pattern to check against ( May contain '' and '?'
 *        wildcards )
 * @param ignoreCase Definition, if case sensitive character comparison or
 *        not.
 * @param digitWildcard A wildcard character that stands as placeholder for
 *        digits
 */
    public StringPattern(String pattern, boolean ignoreCase, char digitWildcard) {
        this.setPattern(pattern);
        this.setIgnoreCase(ignoreCase);
        this.setDigitWildcardChar(digitWildcard);
    }

    // StringPattern()

    /**
 * Returns whether or not the pattern matching ignores upper and lower case
 *
 * @return
 */
    public boolean getIgnoreCase() {
        return ignoreCase;
    }

    /**
 * Sets whether the pattern matching should ignore case or not
 *
 * @param newValue
 */
    public void setIgnoreCase(boolean newValue) {
        ignoreCase = newValue;
    }

    /**
 * Returns the pattern as string.
 *
 * @return
 */
    public String getPattern() {
        return pattern;
    }

    /**
 * Sets the pattern to a new value
 *
 * @param newValue
 */
    public void setPattern(String newValue) {
        pattern = newValue;
    }

    /**
 *
 *
 * @return
 */
    protected Character digitWildcard() {
        return digitWildcard;
    }

    /**
 *
 *
 * @param newValue
 */
    protected void digitWildcard(Character newValue) {
        digitWildcard = newValue;
    }

    // =========================================================================
    // CLASS METHODS
    // =========================================================================

    /**
 * Returns true, if the given probe string matches the given pattern.  <br>
 * The character comparison is done case sensitive.
 *
 * @param probe The string to check against the pattern.
 * @param pattern The patter, that probably contains wildcards ( '' or '?'
 *        )
 *
 * @return
 */
    public static boolean match(String probe, String pattern) {
        StringPattern stringPattern = new StringPattern(pattern, false);

        return (stringPattern.matches(probe));
    }

    // match()
    // -------------------------------------------------------------------------

    /**
 * Returns true, if the given probe string matches the given pattern.  <br>
 * The character comparison is done ignoring upper/lower-case.
 *
 * @param probe The string to check against the pattern.
 * @param pattern The patter, that probably contains wildcards ( '' or '?'
 *        )
 *
 * @return
 */
    public static boolean matchIgnoreCase(String probe, String pattern) {
        StringPattern stringPattern = new StringPattern(pattern, true);

        return (stringPattern.matches(probe));
    }

    // matchIgnoreCase()
    // -------------------------------------------------------------------------
    // =========================================================================
    // PUBLIC INSTANCE METHODS
    // =========================================================================

    /**
 * Tests if a specified string matches the pattern.
 *
 * @param probe The string to compare to the pattern
 *
 * @return true if and only if the probe matches the pattern, false
 *         otherwise.
 */
    public boolean matches(String probe) {
        StringExaminer patternIterator = null;
        StringExaminer probeIterator = null;
        char patternCh = '-';
        char probeCh = '-';
        String newPattern = null;
        String subPattern = null;
        int charIndex = 0;

        if (probe == null) {
            return false;
        }

        if (probe.length() == 0) {
            return false;
        }

        patternIterator = this.newExaminer(this.getPattern());
        probeIterator = this.newExaminer(probe);
        probeCh = probeIterator.nextChar();
        patternCh = this.getPatternChar(patternIterator, probeCh);

        while ((this.endNotReached(patternCh)) &&
                (this.endNotReached(probeCh))) {
            if (patternCh == MULTICHAR_WILDCARD) {
                patternCh = this.skipWildcards(patternIterator);

                if (this.endReached(patternCh)) {
                    return true; // No more characters after multi wildcard - So everything matches
                } else {
                    patternIterator.skip(-1);
                    newPattern = this.upToEnd(patternIterator);
                    charIndex = newPattern.indexOf(MULTICHAR_WILDCARD);

                    if (charIndex >= 0) {
                        subPattern = newPattern.substring(0, charIndex);

                        if (this.skipAfter(probeIterator, subPattern)) {
                            patternIterator = this.newExaminer(newPattern.substring(
                                        charIndex));
                            patternCh = probeCh;
                        } else {
                            return false;
                        }
                    } else {
                        probeIterator.skip(-1);

                        return this.matchReverse(newPattern, probeIterator);
                    }
                }
            }

            if (this.charsAreEqual(probeCh, patternCh)) {
                if (this.endNotReached(patternCh)) {
                    probeCh = probeIterator.nextChar();
                    patternCh = this.getPatternChar(patternIterator, probeCh);
                }
            } else {
                if (patternCh != MULTICHAR_WILDCARD) {
                    return false; // character is not matching - return immediately
                }
            }
        }

        // while()
        return ((this.endReached(patternCh)) && (this.endReached(probeCh)));
    }

    // matches()
    // -------------------------------------------------------------------------

    /**
 * Returns the pattern string.
 *
 * @see java.lang.Object#toString()
 */
    public String toString() {
        if (this.getPattern() == null) {
            return super.toString();
        } else {
            return this.getPattern();
        }
    }

    // toString()
    // -------------------------------------------------------------------------

    /**
 * Returns true if the pattern contains any '' or '?' wildcard character.
 *
 * @return
 */
    public boolean hasWildcard() {
        if (this.getPattern() == null) {
            return false;
        }

        if (this.hasDigitWildcard()) {
            if (this.getPattern().indexOf(this.digitWildcardChar()) >= 0) {
                return true;
            }
        }

        return (this.getPattern().indexOf(MULTI_WILDCARD) >= 0) ||
        (this.getPattern().indexOf(SINGLECHAR_WILDCARD) >= 0);
    }

    // hasWildcard()
    // -------------------------------------------------------------------------

    /**
 * Sets the given character as a wildcard character in this pattern to
 * match only digits ('0'-'9').   <br>
 *
 * @param digitWildcard The placeholder character for digits
 */
    public void setDigitWildcardChar(char digitWildcard) {
        if (digitWildcard <= 0) {
            this.digitWildcard(null);
        } else {
            this.digitWildcard(new Character(digitWildcard));
        }
    }

    // setDigitWildcardChar()

    /**
 *
 *
 * @return
 */
    protected boolean hasDigitWildcard() {
        return this.digitWildcard() != null;
    }

    // hasDigitWildcard()
    // -------------------------------------------------------------------------
    protected char digitWildcardChar() {
        if (this.hasDigitWildcard()) {
            return this.digitWildcard().charValue();
        } else {
            return '\0';
        }
    }

    // digitWildcardChar()
    // -------------------------------------------------------------------------

    /**
 * Moves the iterator position to the next character that is no wildcard.
 * Doesn't skip digit wildcards !
 *
 * @param iterator
 *
 * @return
 */
    protected char skipWildcards(StringExaminer iterator) {
        char result = '-';

        do {
            result = iterator.nextChar();
        } while ((result == MULTICHAR_WILDCARD) ||
                (result == SINGLECHAR_WILDCARD));

        return result;
    }

    // skipWildcards()
    // -------------------------------------------------------------------------

    /**
 * Increments the given iterator up to the last character that matched the
 * character sequence in the given matchString. Returns true, if the
 * matchString was found, otherwise false.
 *
 * @param examiner
 * @param matchString The string to be found (must not contain )
 *
 * @return
 */
    protected boolean skipAfter(StringExaminer examiner, String matchString) {
        // Do not use the method of StringExaminer anymore, because digit wildcard
        // support is in the charsAreEqual() method which is unknown to the examiner.
        // return examiner.skipAfter( matchString ) ;
        char ch = '-';
        char matchChar = ' ';
        boolean found = false;
        int index = 0;

        if ((matchString == null) || (matchString.length() == 0)) {
            return false;
        }

        ch = examiner.nextChar();

        while ((examiner.endNotReached(ch)) && (!found)) {
            matchChar = matchString.charAt(index);

            if (this.charsAreEqual(ch, matchChar)) {
                index++;

                if (index >= matchString.length()) { // whole matchString checked ?
                    found = true;
                } else {
                    ch = examiner.nextChar();
                }
            } else {
                if (index == 0) {
                    ch = examiner.nextChar();
                } else {
                    index = 0;
                }
            }
        }

        return found;
    }

    // skipAfter()
    // -------------------------------------------------------------------------
    protected String upToEnd(StringExaminer iterator) {
        return iterator.upToEnd();
    }

    // upToEnd()
    // -------------------------------------------------------------------------
    protected boolean matchReverse(String pattern, StringExaminer probeIterator) {
        String newPattern;
        String newProbe;
        StringPattern newMatcher;
        newPattern = MULTI_WILDCARD + pattern;
        newProbe = this.upToEnd(probeIterator);
        newPattern = this.strUtil().reverse(newPattern);
        newProbe = this.strUtil().reverse(newProbe);
        newMatcher = new StringPattern(newPattern, this.getIgnoreCase());

        if (this.hasDigitWildcard()) {
            newMatcher.setDigitWildcardChar(this.digitWildcardChar());
        }

        return newMatcher.matches(newProbe);
    }

    // matchReverse()
    // -------------------------------------------------------------------------
    protected boolean charsAreEqual(char probeChar, char patternChar) {
        if (this.hasDigitWildcard()) {
            if (patternChar == this.digitWildcardChar()) {
                return Character.isDigit(probeChar);
            }
        }

        if (this.getIgnoreCase()) {
            return (Character.toUpperCase(probeChar) == Character.toUpperCase(patternChar));
        } else {
            return (probeChar == patternChar);
        }
    }

    // charsAreEqual()
    // -------------------------------------------------------------------------
    protected boolean endReached(char character) {
        return (character == StringExaminer.END_REACHED);
    }

    // endReached()
    // -------------------------------------------------------------------------
    protected boolean endNotReached(char character) {
        return (!endReached(character));
    }

    // endNotReached()
    // -------------------------------------------------------------------------
    protected char getPatternChar(StringExaminer patternIterator, char probeCh) {
        char patternCh;
        patternCh = patternIterator.nextChar();

        return ((patternCh == SINGLECHAR_WILDCARD) ? probeCh : patternCh);
    }

    // getPatternChar()
    // -------------------------------------------------------------------------
    protected StringExaminer newExaminer(String str) {
        return new StringExaminer(str, this.getIgnoreCase());
    }

    // newExaminer()
    // -------------------------------------------------------------------------
    protected StringUtil strUtil() {
        return StringUtil.current();
    }

    // strUtil()
    // -------------------------------------------------------------------------
}


// class StringPattern
