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
package com.sshtools.common.util;

import java.awt.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class PropertyUtil {
    /**
*
*
* @param number
* @param defaultValue
*
* @return
*/
    public static int stringToInt(String number, int defaultValue) {
        try {
            return (number == null) ? defaultValue : Integer.parseInt(number);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
*
*
* @param color
*
* @return
*/
    public static String colorToString(Color color) {
        StringBuffer buf = new StringBuffer();
        buf.append('#');
        buf.append(numberToPaddedHexString(color.getRed(), 2));
        buf.append(numberToPaddedHexString(color.getGreen(), 2));
        buf.append(numberToPaddedHexString(color.getBlue(), 2));

        return buf.toString();
    }

    /**
*
*
* @param font
*
* @return
*/
    public static String fontToString(Font font) {
        StringBuffer b = new StringBuffer(font.getName());
        b.append(",");
        b.append(font.getStyle());
        b.append(",");
        b.append(font.getSize());

        return b.toString();
    }

    /**
*
*
* @param fontString
*
* @return
*/
    public static Font stringToFont(String fontString) {
        StringTokenizer st = new StringTokenizer(fontString, ",");

        try {
            return new Font(st.nextToken(), Integer.parseInt(st.nextToken()),
                Integer.parseInt(st.nextToken()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
*
*
* @param s
*
* @return
*
* @throws IllegalArgumentException
*/
    public static Color stringToColor(String s) {
        try {
            return new Color(Integer.decode("0x" + s.substring(1, 3)).intValue(),
                Integer.decode("0x" + s.substring(3, 5)).intValue(),
                Integer.decode("0x" + s.substring(5, 7)).intValue());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Bad color string format. Should be #rrggbb ");
        }
    }

    /**
*
*
* @param number
* @param size
*
* @return
*
* @throws IllegalArgumentException
*/
    public static String numberToPaddedHexString(int number, int size) {
        String s = Integer.toHexString(number);

        if (s.length() > size) {
            throw new IllegalArgumentException(
                "Number too big for padded hex string");
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < (size - s.length()); i++) {
            buf.append('0');
        }

        buf.append(s);

        return buf.toString();
    }
}
