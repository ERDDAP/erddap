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
public class RecordAction extends StandardAction {
    private final static String ACTION_COMMAND_KEY_RECORD = "record-command";
    private final static String NAME_RECORD = "Record";
    private final static String SMALL_ICON_RECORD = "/com/sshtools/common/ui/record.png";
    private final static String LARGE_ICON_RECORD = "";
    private final static String SHORT_DESCRIPTION_RECORD = "Record output";
    private final static String LONG_DESCRIPTION_RECORD = "Record all output to a file";
    private final static int MNEMONIC_KEY_RECORD = 'R';

    /**
* Creates a new RecordAction object.
*/
    public RecordAction() {
        putValue(Action.NAME, NAME_RECORD);
        putValue(Action.SMALL_ICON, getIcon(SMALL_ICON_RECORD));
        putValue(LARGE_ICON, getIcon(LARGE_ICON_RECORD));
        putValue(Action.SHORT_DESCRIPTION, SHORT_DESCRIPTION_RECORD);
        putValue(Action.LONG_DESCRIPTION, LONG_DESCRIPTION_RECORD);
        putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.ALT_MASK));
        putValue(Action.MNEMONIC_KEY, new Integer(MNEMONIC_KEY_RECORD));
        putValue(Action.ACTION_COMMAND_KEY, ACTION_COMMAND_KEY_RECORD);
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_NAME, "File");
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(60));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(20));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(true));
        putValue(StandardAction.TOOLBAR_GROUP, new Integer(60));
        putValue(StandardAction.TOOLBAR_WEIGHT, new Integer(20));
    }
}
