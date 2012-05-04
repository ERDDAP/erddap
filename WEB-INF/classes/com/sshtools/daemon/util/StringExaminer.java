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
// CONTENT  : CLASS StringExaminer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 29/09/2002
// HISTORY  :
//  29/09/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package com.sshtools.daemon.util;


// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * As a subclass of StringScanner this class allows more advanced navigation
 * over the underlying string.    <br>
 * That includes moving to positions of specific substrings etc.
 *
 * @author Manfred Duchrow
 * @version $Id: StringExaminer.java,v 1.7 2003/09/11 15:37:07 martianx Exp $
 */
public class StringExaminer extends StringScanner {
    // =========================================================================
    // CONSTANTS
    // =========================================================================
    // =========================================================================
    // INSTANCE VARIABLES
    // =========================================================================
    private boolean ignoreCase = false;

    // =========================================================================
    // CLASS METHODS
    // =========================================================================
    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /**
 * Initialize the new instance with the string to examine.   <br>
 * The string will be treated case-sensitive.
 *
 * @param stringToExamine The string that should be examined
 */
    public StringExaminer(String stringToExamine) {
        this(stringToExamine, false);
    }

    // StringExaminer()
    // -------------------------------------------------------------------------

    /**
 * Initialize the new instance with the string to examine.
 *
 * @param stringToExamine The string that should be examined
 * @param ignoreCase Specified whether or not treating the string case
 *        insensitive
 */
    public StringExaminer(String stringToExamine, boolean ignoreCase) {
        super(stringToExamine);
        this.ignoreCase(ignoreCase);
    }

    // StringExaminer()

    /**
 *
 *
 * @return
 */
    protected boolean ignoreCase() {
        return ignoreCase;
    }

    /**
 *
 *
 * @param newValue
 */
    protected void ignoreCase(boolean newValue) {
        ignoreCase = newValue;
    }

    // -------------------------------------------------------------------------
    // =========================================================================
    // PUBLIC INSTANCE METHODS
    // =========================================================================

    /**
 * Increments the position pointer up to the last character that matched
 * the character sequence in the given matchString. Returns true, if the
 * matchString was found, otherwise false.
 *
 * <p>
 * If the matchString was found, the next invocation of method nextChar()
 * returns the first character after that matchString.
 * </p>
 *
 * @param matchString The string to look up
 *
 * @return
 */
    public boolean skipAfter(String matchString) {
        char ch = '-';
        char matchChar = ' ';
        boolean found = false;
        int index = 0;

        if ((matchString == null) || (matchString.length() == 0)) {
            return false;
        }

        ch = this.nextChar();

        while ((endNotReached(ch)) && (!found)) {
            matchChar = matchString.charAt(index);

            if (this.charsAreEqual(ch, matchChar)) {
                index++;

                if (index >= matchString.length()) { // whole matchString checked ?
                    found = true;
                } else {
                    ch = this.nextChar();
                }
            } else {
                if (index == 0) {
                    ch = this.nextChar();
                } else {
                    index = 0;
                }
            }
        }

        return found;
    }

    // skipAfter()
    // -------------------------------------------------------------------------

    /**
 * Increments the position pointer up to the first character before the
 * character sequence in the given matchString. Returns true, if the
 * matchString was found, otherwise false.
 *
 * <p>
 * If the matchString was found, the next invocation of method nextChar()
 * returns the first character of that matchString from the position where
 * it was found inside the examined string.
 * </p>
 *
 * @param matchString The string to look up
 *
 * @return
 */
    public boolean skipBefore(String matchString) {
        boolean found;
        found = this.skipAfter(matchString);

        if (found) {
            this.skip(0 - matchString.length());
        }

        return found;
    }

    // skipBefore()
    // -------------------------------------------------------------------------

    /**
 * Returns the a string containing all characters from the current position
 * up to the end of the examined string.   <br>
 * The character position of the examiner is not changed by this method.
 *
 * @return
 */
    public String peekUpToEnd() {
        return this.upToEnd(true);
    }

    // peekUpToEnd()
    // -------------------------------------------------------------------------

    /**
 * Returns the a string containing all characters from the current position
 * up to the end of the examined string.   <br>
 * The character position is put to the end by this method. That means the
 * next invocation of nextChar() returns END_REACHED.
 *
 * @return
 */
    public String upToEnd() {
        return this.upToEnd(false);
    }

    // upToEnd()

    /**
 *
 *
 * @param char1
 * @param char2
 *
 * @return
 */
    protected boolean charsAreEqual(char char1, char char2) {
        return (this.ignoreCase())
        ? (Character.toUpperCase(char1) == Character.toUpperCase(char2))
        : (char1 == char2);
    }

    // charsAreEqual()
    // -------------------------------------------------------------------------

    /**
 * Returns the a string containing all characters from the current position
 * up to the end of the examined string.   <br>
 * Depending on the peek flag the character position of the examiner  is
 * unchanged (true) after calling this method or points behind the strings
 * last character.
 *
 * @param peek
 *
 * @return
 */
    protected String upToEnd(boolean peek) {
        char result = '-';
        int lastPosition = 0;
        StringBuffer buffer = new StringBuffer(100);
        lastPosition = this.getPosition();
        result = this.nextChar();

        while (endNotReached(result)) {
            buffer.append(result);
            result = this.nextChar();
        }

        if (peek) {
            this.setPosition(lastPosition);
        }

        return buffer.toString();
    }

    // upToEnd()
    // -------------------------------------------------------------------------
}


// class StringExaminer
