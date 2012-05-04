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

import java.awt.event.*;

import java.net.*;

import javax.swing.*;
import javax.swing.event.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.16 $
 */
public abstract class StandardAction extends AbstractAction {
    /**  */
    public final static String ON_TOOLBAR = "onToolBar";

    /**  */
    public final static String TOOLBAR_GROUP = "toolBarGroup";

    /**  */
    public final static String TOOLBAR_WEIGHT = "toolBarWeight";

    /**  */
    public final static String ON_MENUBAR = "onMenuBar";

    /**  */
    public final static String MENU_NAME = "menuName";

    /**  */
    public final static String MENU_ITEM_GROUP = "menuItemGroup";

    /**  */
    public final static String MENU_ITEM_WEIGHT = "menuItemWeight";

    /**  */
    public final static String IMAGE_DIR = "/com/sshtools/sshterm/";

    /**  */
    public final static String HIDE_TOOLBAR_TEXT = "hideToolbarText";

    /**  */
    public final static String IS_TOGGLE_BUTTON = "isToggleButton";

    /**  */
    public final static String LARGE_ICON = "LargeIcon";

    /**  */
    public final static String ON_CONTEXT_MENU = "onContextMenu";

    /**  */
    public final static String CONTEXT_MENU_GROUP = "contextMenuGroup";

    /**  */
    public final static String CONTEXT_MENU_WEIGHT = "contextMenuWeight";

    /**  */
    public final static String MENU_ICON = "menuIcon";

    // The listener to action events (usually the main UI)
    private EventListenerList listeners;

    /**
*
*
* @return
*/
    public String getActionCommand() {
        return (String) getValue(Action.ACTION_COMMAND_KEY);
    }

    /**
*
*
* @return
*/
    public String getShortDescription() {
        return (String) getValue(Action.SHORT_DESCRIPTION);
    }

    /**
*
*
* @return
*/
    public String getLongDescription() {
        return (String) getValue(Action.LONG_DESCRIPTION);
    }

    /**
*
*
* @return
*/
    public String getName() {
        return (String) getValue(Action.NAME);
    }

    /**
*
*
* @return
*/
    public String getSmallIcon() {
        return (String) getValue(Action.SMALL_ICON);
    }

    /**
*
*
* @param evt
*/
    public void actionPerformed(ActionEvent evt) {
        if (listeners != null) {
            Object[] listenerList = listeners.getListenerList();

            // Recreate the ActionEvent and stuff the value of the ACTION_COMMAND_KEY
            ActionEvent e = new ActionEvent(evt.getSource(), evt.getID(),
                    (String) getValue(Action.ACTION_COMMAND_KEY));

            for (int i = 0; i <= (listenerList.length - 2); i += 2) {
                ((ActionListener) listenerList[i + 1]).actionPerformed(e);
            }
        }
    }

    /**
*
*
* @param l
*/
    public void addActionListener(ActionListener l) {
        if (listeners == null) {
            listeners = new EventListenerList();
        }

        listeners.add(ActionListener.class, l);
    }

    /**
*
*
* @param l
*/
    public void removeActionListener(ActionListener l) {
        if (listeners == null) {
            return;
        }

        listeners.remove(ActionListener.class, l);
    }

    /**
*
*
* @param name
*
* @return
*/
    public ImageIcon getIcon(String name) {
        String imagePath = name.startsWith("/") ? name : (IMAGE_DIR + name);
        URL url = this.getClass().getResource(imagePath);

        if (url != null) {
            return new ImageIcon(url);
        }

        return null;
    }
}
