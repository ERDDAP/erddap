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

import javax.swing.Action;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class SaveAsAction extends StandardAction {
    /**
* Creates a new SaveAsAction object.
*/
    public SaveAsAction() {
        putValue(Action.NAME, "Save As");
        putValue(Action.SMALL_ICON, new EmptyIcon(16, 16));
        putValue(Action.SHORT_DESCRIPTION, "Save the connection");
        putValue(Action.LONG_DESCRIPTION,
            "Save the connection as a different file");
        putValue(Action.MNEMONIC_KEY, new Integer('v'));
        putValue(Action.ACTION_COMMAND_KEY, "saveas-command");
        putValue(StandardAction.MENU_NAME, "File");
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(0));
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(51));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(false));
    }
}
