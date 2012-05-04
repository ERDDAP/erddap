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

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class AboutAction extends StandardAction {
    private final static String ACTION_COMMAND_KEY_ABOUT = "about-command";
    private final static String NAME_ABOUT = "About";
    private final static String SMALL_ICON_ABOUT = "/com/sshtools/common/ui/about.png";
    private final static String LARGE_ICON_ABOUT = "";
    private final static int MNEMONIC_KEY_ABOUT = 'A';
    private SshToolsApplication application;
    private Component parent;

    /**
* Creates a new AboutAction object.
*
* @param parent
* @param application
*/
    public AboutAction(Component parent, SshToolsApplication application) {
        this.application = application;
        this.parent = parent;
        putValue(Action.NAME, NAME_ABOUT);
        putValue(Action.SMALL_ICON, getIcon(SMALL_ICON_ABOUT));
        putValue(LARGE_ICON, getIcon(LARGE_ICON_ABOUT));
        putValue(Action.SHORT_DESCRIPTION,
            "About " + application.getApplicationName());
        putValue(Action.LONG_DESCRIPTION,
            "Show information about " + application.getApplicationName());
        putValue(Action.MNEMONIC_KEY, new Integer(MNEMONIC_KEY_ABOUT));
        putValue(Action.ACTION_COMMAND_KEY, ACTION_COMMAND_KEY_ABOUT);
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_NAME, "Help");
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(90));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(90));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(true));
        putValue(StandardAction.TOOLBAR_GROUP, new Integer(90));
        putValue(StandardAction.TOOLBAR_WEIGHT, new Integer(10));
    }

    /**
*
*
* @param evt
*/
    public void actionPerformed(ActionEvent evt) {
        application.showAbout(parent);
    }
}
