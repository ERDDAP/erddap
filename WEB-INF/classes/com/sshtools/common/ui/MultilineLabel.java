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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.util.StringTokenizer;

import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class MultilineLabel extends JPanel {
    //  Private instance variables
    private GridBagConstraints constraints;
    private String text;

    /**
* Creates a new MultilineLabel object.
*/
    public MultilineLabel() {
        this("");
    }

    /**
* Creates a new MultilineLabel object.
*
* @param text
*/
    public MultilineLabel(String text) {
        super(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;
        setText(text);
    }

    /**
*
*
* @param f
*/
    public void setFont(Font f) {
        super.setFont(f);

        for (int i = 0; i < getComponentCount(); i++) {
            getComponent(i).setFont(f);
        }
    }

    /**
*
*
* @param text
*/
    public void setText(String text) {
        this.text = text;
        removeAll();

        StringTokenizer tok = new StringTokenizer(text, "\n");
        constraints.weighty = 0.0;
        constraints.weightx = 1.0;

        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();

            if (!tok.hasMoreTokens()) {
                constraints.weighty = 1.0;
            }

            UIUtil.jGridBagAdd(this, new JLabel(t), constraints,
                GridBagConstraints.REMAINDER);
        }

        revalidate();
        repaint();
    }

    /**
*
*
* @return
*/
    public String getText() {
        return text;
    }
}
