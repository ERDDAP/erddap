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

import com.sshtools.j2ssh.util.StartStopState;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public class StartStopStateRenderer extends DefaultTableCellRenderer {
    //  Private instance variables
    private Icon startedIcon;

    //  Private instance variables
    private Icon stoppedIcon;

    //  Private instance variables
    private Icon failedIcon;

    //  Private instance variables
    private String errorMsg;

    public StartStopStateRenderer(Icon startedIcon, Icon stoppedIcon) {
        this.startedIcon = startedIcon;
        this.stoppedIcon = stoppedIcon;
        setHorizontalAlignment(JLabel.CENTER);
    }

    public StartStopStateRenderer(Icon startedIcon, Icon stoppedIcon,
        Icon failedIcon, String errorMsg) {
        this.startedIcon = startedIcon;
        this.stoppedIcon = stoppedIcon;
        this.failedIcon = failedIcon;
        this.errorMsg = errorMsg;
        setHorizontalAlignment(JLabel.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
            row, column);

        StartStopState state = (StartStopState) value;

        if (state.getValue() == StartStopState.FAILED) {
            setIcon(failedIcon);
            setToolTipText(errorMsg);
        } else {
            setIcon((state.getValue() == StartStopState.STOPPED) ? stoppedIcon
                                                                 : startedIcon);
            setToolTipText(null);
        }

        return this;
    }

    public String getText() {
        return null;
    }
}
