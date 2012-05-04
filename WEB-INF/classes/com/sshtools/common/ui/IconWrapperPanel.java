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
 * @version $Revision: 1.14 $
 */
public class IconWrapperPanel extends JPanel {
    /**
* Creates a new IconWrapperPanel object.
*
* @param icon
* @param component
*/
    public IconWrapperPanel(Icon icon, Component component) {
        super(new BorderLayout());

        //  Create the west panel with the icon in it
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        westPanel.add(new JLabel(icon), BorderLayout.NORTH);

        //  Build this panel
        add(westPanel, BorderLayout.WEST);
        add(component, BorderLayout.CENTER);
    }
}
