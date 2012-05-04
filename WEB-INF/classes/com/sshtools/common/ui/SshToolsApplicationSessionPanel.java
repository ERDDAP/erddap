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

import com.sshtools.common.configuration.SshToolsConnectionProfile;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.connection.ChannelEventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.LayoutManager;

import java.io.File;
import java.io.IOException;

import java.util.Comparator;

import javax.swing.SwingConstants;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public abstract class SshToolsApplicationSessionPanel
    extends SshToolsApplicationPanel {
    /**  */
    public final static String PREF_CONNECTION_FILE_DIRECTORY = "sshapps.connectionFile.directory";

    /**  */
    protected Log log = LogFactory.getLog(SshToolsApplicationSessionPanel.class);

    /**  */
    protected SshToolsConnectionProfile currentConnectionProfile;

    /**  */
    protected SessionManager manager;

    /**
* Creates a new SshToolsApplicationClientPanel object.
*/
    public SshToolsApplicationSessionPanel() {
        super();
    }

    /**
* Creates a new SshToolsApplicationClientPanel object.
*
* @param mgr
*/
    public SshToolsApplicationSessionPanel(LayoutManager mgr) {
        super(mgr);
    }

    /**
*
*
* @return
*/
    public abstract SshToolsConnectionTab[] getAdditionalConnectionTabs();

    public abstract void addEventListener(ChannelEventListener eventListener);

    public abstract boolean requiresConfiguration();

    public abstract String getId();

    /**
*
*
* @param manager
* @param profile
*
* @throws IOException
*/
    public final boolean openSession(SessionManager manager,
        SshToolsConnectionProfile profile) throws IOException {
        this.manager = manager;

        // Set the current connection properties
        setCurrentConnectionProfile(profile);

        if (requiresConfiguration() &&
                !profile.getApplicationPropertyBoolean(getId() + ".configured",
                    false)) {
            if (!editSettings(profile)) {
                return false;
            }
        }

        return onOpenSession();
    }

    /**
*
*
* @throws IOException
*/
    public abstract boolean onOpenSession() throws IOException;

    /**
*
*
* @return
*/
    public boolean isConnected() {
        return (manager != null) && manager.isConnected();
    }

    /**
*
*
* @param file
*/
    public void setContainerTitle(File file) {
        String verString = "";

        if (application != null) {
            verString = ConfigurationLoader.getVersionString(application.getApplicationName(),
                    application.getApplicationVersion());
        }

        if (container != null) {
            container.setContainerTitle((file == null) ? verString
                                                       : (verString + " [" +
                file.getName() + "]"));
        }
    }

    /**
*
*
* @param profile
*/
    public void setCurrentConnectionProfile(SshToolsConnectionProfile profile) {
        currentConnectionProfile = profile;
    }

    /**
*
*
* @return
*/
    public SshToolsConnectionProfile getCurrentConnectionProfile() {
        return currentConnectionProfile;
    }

    /**
*
*
* @param profile
*
* @return
*/
    public boolean editSettings(SshToolsConnectionProfile profile) {
        final SshToolsConnectionPanel panel = new SshToolsConnectionPanel(false);
        SshToolsConnectionTab[] tabs = getAdditionalConnectionTabs();

        for (int i = 0; (tabs != null) && (i < tabs.length); i++) {
            tabs[i].setConnectionProfile(profile);
            panel.addTab(tabs[i]);
        }

        panel.setConnectionProfile(profile);

        final Option ok = new Option("Ok",
                "Apply the settings and close this dialog", 'o');
        final Option cancel = new Option("Cancel",
                "Close this dialog without applying the settings", 'c');
        OptionCallback callback = new OptionCallback() {
                public boolean canClose(OptionsDialog dialog, Option option) {
                    if (option == ok) {
                        return panel.validateTabs();
                    }

                    return true;
                }
            };

        OptionsDialog od = OptionsDialog.createOptionDialog(SshToolsApplicationSessionPanel.this,
                new Option[] { ok, cancel }, panel, "Connection Settings", ok,
                callback, null);
        od.pack();
        UIUtil.positionComponent(SwingConstants.CENTER, od);
        od.setVisible(true);

        if (od.getSelectedOption() == ok) {
            // Apply the changes to the profile
            panel.applyTabs();

            // Ask the session manager to apply them to persistence
            manager.applyProfileChanges(profile);

            return true;
        }

        return false;
    }

    /*public static class ActionMenu
implements Comparable {
int weight;
int mnemonic;
String name;
String displayName;
public ActionMenu(String name, String displayName, int mnemonic,
                int weight) {
this.name = name;
this.displayName = displayName;
this.mnemonic = mnemonic;
this.weight = weight;
}
public int compareTo(Object o) {
int i = new Integer(weight).compareTo(new Integer(
    ( (ActionMenu) o).weight));
return (i == 0)
    ? displayName.compareTo( ( (ActionMenu) o).displayName) : i;
}
}*/
    class ToolBarActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i = ((Integer) ((StandardAction) o1).getValue(StandardAction.TOOLBAR_GROUP)).compareTo((Integer) ((StandardAction) o2).getValue(
                        StandardAction.TOOLBAR_GROUP));

            return (i == 0)
            ? ((Integer) ((StandardAction) o1).getValue(StandardAction.TOOLBAR_WEIGHT)).compareTo((Integer) ((StandardAction) o2).getValue(
                    StandardAction.TOOLBAR_WEIGHT)) : i;
        }
    }

    class MenuItemActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i = ((Integer) ((StandardAction) o1).getValue(StandardAction.MENU_ITEM_GROUP)).compareTo((Integer) ((StandardAction) o2).getValue(
                        StandardAction.MENU_ITEM_GROUP));

            return (i == 0)
            ? ((Integer) ((StandardAction) o1).getValue(StandardAction.MENU_ITEM_WEIGHT)).compareTo((Integer) ((StandardAction) o2).getValue(
                    StandardAction.MENU_ITEM_WEIGHT)) : i;
        }
    }
}
