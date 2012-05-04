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
 * @version $Revision: 1.15 $
 */
public class ToolButton extends JButton {
    //
    private final static Insets INSETS = new Insets(0, 0, 0, 0);
    private boolean hideText;

    /**
* Creates a new ToolButton object.
*
* @param action
*/
    public ToolButton(Action action) {
        super(action);
        setMargin(INSETS);
        setRequestFocusEnabled(false);
        setFocusPainted(false);

        if (action.getValue(StandardAction.HIDE_TOOLBAR_TEXT) != null) {
            setHideText(Boolean.TRUE.equals(
                    (Boolean) action.getValue(StandardAction.HIDE_TOOLBAR_TEXT)));
        } else {
            setHideText(true);
        }
    }

    /**
*
*
* @return
*/
    public boolean isFocusable() {
        return false;
    }

    /**
*
*
* @param hideText
*/
    public void setHideText(boolean hideText) {
        if (this.hideText != hideText) {
            firePropertyChange("hideText", this.hideText, hideText);
        }

        this.hideText = hideText;
        this.setHorizontalTextPosition(ToolButton.RIGHT);
        repaint();
    }

    /**
*
*
* @return
*/
    public String getText() {
        return hideText ? null : super.getText();
    }
}
