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
 *  Sshtools - SSHTerm
 *
 *  Copyright (C) 2002 Lee David Painter.
 *
 *  Written by: 2002 Lee David Painter <lee@sshtools.com>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.sshtools.common.ui;

import java.awt.*;

import javax.swing.*;


public class FolderBar extends JPanel {
    //  Private instance variables
    private JLabel textLabel;

    //  Private instance variables
    private JLabel iconLabel;
    private Action action;

    public FolderBar() {
        this(null, null);
    }

    public FolderBar(String text) {
        this(text, null);
    }

    public FolderBar(String text, Icon icon) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(getBackground().darker());
        add(textLabel = new JLabel(), BorderLayout.CENTER);
        add(iconLabel = new JLabel(), BorderLayout.WEST);
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD));
        textLabel.setVerticalAlignment(JLabel.CENTER);
        textLabel.setVerticalTextPosition(JLabel.BOTTOM);
        textLabel.setForeground(Color.lightGray);
        iconLabel.setVerticalAlignment(JLabel.CENTER);
        setIcon(icon);
        setText(text);
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
        setIcon((Icon) action.getValue(Action.SMALL_ICON));
        setText((String) action.getValue(Action.NAME));
    }

    public void setText(String text) {
        textLabel.setText(text);
    }

    public void setIcon(Icon icon) {
        iconLabel.setIcon(icon);
    }
}
