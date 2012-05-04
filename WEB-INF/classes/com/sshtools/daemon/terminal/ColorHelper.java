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

/**
 * SSHTools - Java SSH API The contents of this package has been derived from
 * the TelnetD library available from http://sourceforge.net/projects/telnetd
 * The original license of the source code is as follows: TelnetD library
 * (embeddable telnet daemon) Copyright (C) 2000 Dieter Wimberger This library
 * is free software; you can either redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 2.1,1999 as
 * published by the Free Software Foundation (see copy received along with the
 * library), or under the terms of the BSD-style license received along with
 * this library.
 */
package com.sshtools.daemon.terminal;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class ColorHelper {
    /**  */
    public static final String INTERNAL_MARKER = "\001";

    /**  */
    public static final int MARKER_CODE = 1;

    /**  */
    public static final String BLACK = "S";

    /**  */
    public static final String RED = "R";

    /**  */
    public static final String GREEN = "G";

    /**  */
    public static final String YELLOW = "Y";

    /**  */
    public static final String BLUE = "B";

    /**  */
    public static final String MAGENTA = "M";

    /**  */
    public static final String CYAN = "C";

    /**  */
    public static final String white = "W";

    /**  */
    public static final String BOLD = "f";

    /**  */
    public static final String BOLD_OFF = "d"; //normal color or normal intensity

    /**  */
    public static final String ITALIC = "i";

    /**  */
    public static final String ITALIC_OFF = "j";

    /**  */
    public static final String UNDERLINED = "u";

    /**  */
    public static final String UNDERLINED_OFF = "v";

    /**  */
    public static final String BLINK = "e";

    /**  */
    public static final String BLINK_OFF = "n";

    /**  */
    public static final String RESET_ALL = "a";

    /**
 *
 *
 * @param str
 * @param color
 *
 * @return
 */
    public static String colorizeText(String str, String color) {
        return INTERNAL_MARKER + color + str + INTERNAL_MARKER + RESET_ALL;
    }

    //colorizeText
    public static String colorizeBackground(String str, String color) {
        return INTERNAL_MARKER + color.toLowerCase() + str + INTERNAL_MARKER +
        RESET_ALL;
    }

    //colorizeBackground
    public static String colorizeText(String str, String fgc, String bgc) {
        return INTERNAL_MARKER + fgc + INTERNAL_MARKER + bgc.toLowerCase() +
        str + INTERNAL_MARKER + RESET_ALL;
    }

    //colorizeText
    public static String boldcolorizeText(String str, String color) {
        return INTERNAL_MARKER + BOLD + INTERNAL_MARKER + color + str +
        INTERNAL_MARKER + RESET_ALL;
    }

    //colorizeBoldText
    public static String boldcolorizeText(String str, String fgc, String bgc) {
        return INTERNAL_MARKER + BOLD + INTERNAL_MARKER + fgc +
        INTERNAL_MARKER + bgc.toLowerCase() + str + INTERNAL_MARKER +
        RESET_ALL;
    }

    //colorizeBoldText
    public static String boldText(String str) {
        return INTERNAL_MARKER + BOLD + str + INTERNAL_MARKER + BOLD_OFF;
    }

    //boldText
    public static String italicText(String str) {
        return INTERNAL_MARKER + ITALIC + str + INTERNAL_MARKER + ITALIC_OFF;
    }

    //italicText
    public static String underlinedText(String str) {
        return INTERNAL_MARKER + UNDERLINED + str + INTERNAL_MARKER +
        UNDERLINED_OFF;
    }

    //underlinedText
    public static String blinkingText(String str) {
        return INTERNAL_MARKER + BLINK + str + INTERNAL_MARKER + BLINK_OFF;
    }

    //blinkingText
    public static long getVisibleLength(String str) {
        int counter = 0;
        int parsecursor = 0;
        int foundcursor = 0;
        boolean done = false;

        while (!done) {
            foundcursor = str.indexOf(MARKER_CODE, parsecursor);

            if (foundcursor != -1) {
                //increment counter
                counter++;

                //parseon from the next char
                parsecursor = foundcursor + 1;
            } else {
                done = true;
            }
        }

        return (str.length() - (counter * 2));
    }

    //getVisibleLength
}


//class ColorHelper
