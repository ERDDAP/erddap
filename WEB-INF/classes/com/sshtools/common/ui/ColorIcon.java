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
package com.sshtools.common.ui;

import java.awt.*;

import javax.swing.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class ColorIcon implements Icon {
    //  Private instance variables
    private Dimension size;
    private Color color;
    private Color borderColor;

    /**
* Creates a new ColorIcon object.
*/
    public ColorIcon() {
        this(null);
    }

    /**
* Creates a new ColorIcon object.
*
* @param color
*/
    public ColorIcon(Color color) {
        this(color, null);
    }

    /**
* Creates a new ColorIcon object.
*
* @param color
* @param borderColor
*/
    public ColorIcon(Color color, Color borderColor) {
        this(color, null, borderColor);
    }

    /**
* Creates a new ColorIcon object.
*
* @param color
* @param size
* @param borderColor
*/
    public ColorIcon(Color color, Dimension size, Color borderColor) {
        setColor(color);
        setSize(size);
        setBorderColor(borderColor);
    }

    /**
*
*
* @param c
* @param g
* @param x
* @param y
*/
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor((color == null) ? Color.black : color);
        g.fillRect(x, y, getIconWidth(), getIconHeight());

        if (borderColor != null) {
            g.setColor(borderColor);
            g.drawRect(x, y, getIconWidth(), getIconHeight());
        }
    }

    /**
*
*
* @param size
*/
    public void setSize(Dimension size) {
        this.size = size;
    }

    /**
*
*
* @param color
*/
    public void setColor(Color color) {
        this.color = color;
    }

    /**
*
*
* @param borderColor
*/
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    /**
*
*
* @return
*/
    public int getIconWidth() {
        return (size == null) ? 16 : size.width;
    }

    /**
*
*
* @return
*/
    public int getIconHeight() {
        return (size == null) ? 16 : size.height;
    }
}
