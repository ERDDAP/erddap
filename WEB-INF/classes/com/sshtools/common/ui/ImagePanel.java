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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JPanel;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.17 $
 */
public class ImagePanel extends JPanel {
    private ResourceIcon icon;
    private boolean alignBottomRight = false;

    /**
* Creates a new ImagePanel object.
*
* @param imageName
*/
    public ImagePanel(String imageName) {
        icon = new ResourceIcon(imageName);
    }

    /**
* Creates a new ImagePanel object.
*
* @param icon
* @param bottom
*/
    public ImagePanel(ResourceIcon icon, int bottom) {
        this.icon = icon;
        alignBottomRight = true;
    }

    /**
* Creates a new ImagePanel object.
*
* @param imageName
* @param bottom
*/
    public ImagePanel(String imageName, int bottom) {
        icon = new ResourceIcon(imageName);
        alignBottomRight = true;
    }

    /**
* Creates a new ImagePanel object.
*/
    public ImagePanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
*
*
* @return
*/
    public Dimension getPreferedSize() {
        Insets insets = getInsets();

        return new Dimension(icon.getIconWidth() + insets.left + insets.right,
            icon.getIconHeight() + insets.top + insets.bottom);
    }

    /**
*
*
* @param g
*/
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (icon != null) {
            Insets insets = getInsets();

            if (!alignBottomRight) {
                // Paint the image at the top left hand side of the panel
                icon.paintIcon(this, g, insets.left, insets.top);
            } else {
                // Paint the image at the bottom right hand side of the panel
                icon.paintIcon(this, g,
                    (this.getWidth() - icon.getIconWidth()),
                    (this.getHeight() - icon.getIconHeight()));
            }
        }
    }

    private void jbInit() throws Exception {
        this.setBackground(Color.white);
    }
}
