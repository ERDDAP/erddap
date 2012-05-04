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

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class RefreshAction extends StandardAction {
    private final static String ACTION_COMMAND_KEY_REFRESH = "refresh-command";
    private final static String NAME_REFRESH = "Refresh";
    private final static String SMALL_ICON_REFRESH = "/com/sshtools/common/ui/refresh.png";
    private final static String LARGE_ICON_REFRESH = "";
    private final static String SHORT_DESCRIPTION_REFRESH = "Refresh terminal";
    private final static String LONG_DESCRIPTION_REFRESH = "Refresh the terminal screen";
    private final static int MNEMONIC_KEY_REFRESH = 'R';

    /**
* Creates a new RefreshAction object.
*/
    public RefreshAction() {
        putValue(Action.NAME, NAME_REFRESH);
        putValue(Action.SMALL_ICON, getIcon(SMALL_ICON_REFRESH));
        putValue(LARGE_ICON, getIcon(LARGE_ICON_REFRESH));
        putValue(Action.SHORT_DESCRIPTION, SHORT_DESCRIPTION_REFRESH);
        putValue(Action.LONG_DESCRIPTION, LONG_DESCRIPTION_REFRESH);
        putValue(Action.MNEMONIC_KEY, new Integer(MNEMONIC_KEY_REFRESH));
        putValue(Action.ACTION_COMMAND_KEY, ACTION_COMMAND_KEY_REFRESH);
        putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_F5, KeyEvent.ALT_MASK));
        putValue(StandardAction.MENU_NAME, "View");
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(20));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(10));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(true));
        putValue(StandardAction.TOOLBAR_GROUP, new Integer(5));
        putValue(StandardAction.TOOLBAR_WEIGHT, new Integer(20));
    }
}
