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
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.16 $
 */
public class UIUtil implements SwingConstants {
    /**
* Parse a string in the format of <code>[character]</code> to create an
* Integer that may be used for an action.
*
* @param character mnemonic string
* @return mnemonic
*/
    public static Integer parseMnemonicString(String string) {
        try {
            return new Integer(string);
        } catch (Throwable t) {
            return new Integer(-1);
        }
    }

    /**
* Parse a string in the format of [ALT+|CTRL+|SHIFT+]<keycode> to
* create a keystroke. This can be used to define accelerators from
* resource bundles
*
* @param string accelerator string
* @return keystroke
*/
    public static KeyStroke parseAcceleratorString(String string) {
        if ((string == null) || string.equals("")) {
            return null;
        }

        StringTokenizer t = new StringTokenizer(string, "+");
        int mod = 0;
        int key = -1;

        while (t.hasMoreTokens()) {
            String x = t.nextToken();

            if (x.equalsIgnoreCase("ctrl")) {
                mod += KeyEvent.CTRL_MASK;
            } else if (x.equalsIgnoreCase("shift")) {
                mod += KeyEvent.SHIFT_MASK;
            } else if (x.equalsIgnoreCase("alt")) {
                mod += KeyEvent.ALT_MASK;
            } else {
                try {
                    java.lang.reflect.Field f = KeyEvent.class.getField(x);
                    key = f.getInt(null);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (key != -1) {
            KeyStroke ks = KeyStroke.getKeyStroke(key, mod);

            return ks;
        }

        return null;
    }

    /**
*
*
* @param parent
* @param componentToAdd
* @param constraints
* @param pos
*
* @throws IllegalArgumentException
*/
    public static void jGridBagAdd(JComponent parent, Component componentToAdd,
        GridBagConstraints constraints, int pos) {
        if (!(parent.getLayout() instanceof GridBagLayout)) {
            throw new IllegalArgumentException(
                "parent must have a GridBagLayout");
        }

        //
        GridBagLayout layout = (GridBagLayout) parent.getLayout();

        //
        constraints.gridwidth = pos;
        layout.setConstraints(componentToAdd, constraints);
        parent.add(componentToAdd);
    }

    /**
*
*
* @param p
* @param c
*/
    public static void positionComponent(int p, Component c) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        switch (p) {
        case NORTH_WEST:
            c.setLocation(0, 0);

            break;

        case NORTH:
            c.setLocation((d.width - c.getSize().width) / 2, 0);

            break;

        case NORTH_EAST:
            c.setLocation((d.width - c.getSize().width), 0);

            break;

        case WEST:
            c.setLocation(0, (d.height - c.getSize().height) / 2);

            break;

        case SOUTH_WEST:
            c.setLocation(0, (d.height - c.getSize().height));

            break;

        case EAST:
            c.setLocation(d.width - c.getSize().width,
                (d.height - c.getSize().height) / 2);

            break;

        case SOUTH_EAST:
            c.setLocation((d.width - c.getSize().width),
                (d.height - c.getSize().height) - 30);

            break;

        case CENTER:
            c.setLocation((d.width - c.getSize().width) / 2,
                (d.height - c.getSize().height) / 2);

            break;
        }
    }
}
