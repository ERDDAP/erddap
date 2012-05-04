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
/*
 * Created on 01-Aug-2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.sshtools.common.util;

import java.awt.*;


/**
 * Other utilities not worth their own class
 */
public final class GeneralUtil {
    /**
* This method will replace '&' with "&amp;", '"' with "&quot;", '<' with "&lt;" and '>' with "&gt;".
*
* @param html html to encode
* @return encoded html
*/
    public static String encodeHTML(String html) {
        // Does java have a method of doing this?
        StringBuffer buf = new StringBuffer();
        char ch;

        for (int i = 0; i < html.length(); i++) {
            ch = html.charAt(i);

            switch (ch) {
            case '&':

                //	May be already encoded
                if (((i + 5) < html.length()) &&
                        html.substring(i + 1, i + 5).equals("amp;")) {
                    buf.append(ch);
                } else {
                    buf.append("&amp;");
                }

                break;

            case '"':
                buf.append("&quot;");

                break;

            case '<':
                buf.append("&lt;");

                break;

            case '>':
                buf.append("&gt;");

                break;

            default:
                buf.append(ch);
            }
        }

        return buf.toString();
    }

    /**
* Return a <code>Color</code> object given a string representation of it
*
* @param color
* @return string representation
* @throws IllegalArgumentException if string in bad format
*/
    public static Color stringToColor(String s) {
        try {
            return new Color(Integer.decode("0x" + s.substring(1, 3)).intValue(),
                Integer.decode("0x" + s.substring(3, 5)).intValue(),
                Integer.decode("0x" + s.substring(5, 7)).intValue());
        } catch (Exception e) {
            return null;
        }
    }

    /**
* Return a string representation of a color
*
* @param color
* @return string representation
*/
    public static String colorToString(Color color) {
        if (color == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        buf.append('#');
        buf.append(numberToPaddedHexString(color.getRed(), 2));
        buf.append(numberToPaddedHexString(color.getGreen(), 2));
        buf.append(numberToPaddedHexString(color.getBlue(), 2));

        return buf.toString();
    }

    /**
* Convert a number to a zero padded hex string
*
* @param int number
* @return zero padded hex string
* @throws IllegalArgumentException if number takes up more characters than
*                                  <code>size</code>
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
