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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class SshToolsConnectionPanel extends JPanel {
    //  Strings
    final static String DEFAULT = "<Default>";
    final static int DEFAULT_PORT = 22;

    //

    /**  */
    protected Log log = LogFactory.getLog(SshToolsConnectionPanel.class);

    //
    private Tabber tabber;
    private SshToolsConnectionProfile profile;

    /**
* Creates a new SshToolsConnectionPanel object.
*
* @param showConnectionTabs
*/
    public SshToolsConnectionPanel(boolean showConnectionTabs) {
        super();
        tabber = new Tabber();

        if (showConnectionTabs) {
            //  Add the common tabs
            addTab(new SshToolsConnectionHostTab());
            addTab(new SshToolsConnectionProtocolTab());
            addTab(new SshToolsConnectionProxyTab());
        }

        //  Build this panel
        setLayout(new GridLayout(1, 1));
        add(tabber);
    }

    /**
*
*
* @return
*/
    public boolean validateTabs() {
        return tabber.validateTabs();
    }

    /**
*
*/
    public void applyTabs() {
        tabber.applyTabs();
    }

    /**
*
*
* @param tab
*/
    public void addTab(SshToolsConnectionTab tab) {
        tabber.addTab(tab);
    }

    /**
*
*
* @param profile
*/
    public void setConnectionProfile(SshToolsConnectionProfile profile) {
        this.profile = profile;

        for (int i = 0; i < tabber.getTabCount(); i++) {
            ((SshToolsConnectionTab) tabber.getTabAt(i)).setConnectionProfile(profile);
        }
    }

    /**
*
*
* @param parent
* @param optionalTabs
*
* @return
*/
    public static SshToolsConnectionProfile showConnectionDialog(
        Component parent, SshToolsConnectionTab[] optionalTabs) {
        return showConnectionDialog(parent, null, optionalTabs);
    }

    /**
*
*
* @param parent
* @param profile
* @param optionalTabs
*
* @return
*/
    public static SshToolsConnectionProfile showConnectionDialog(
        Component parent, SshToolsConnectionProfile profile,
        SshToolsConnectionTab[] optionalTabs) {
        //  If no properties are provided, then use the default
        if (profile == null) {
            profile = new SshToolsConnectionProfile();
            profile.setHost(PreferencesStore.get(
                    SshToolsApplication.PREF_CONNECTION_LAST_HOST, ""));
            profile.setPort(PreferencesStore.getInt(
                    SshToolsApplication.PREF_CONNECTION_LAST_PORT, DEFAULT_PORT));
            profile.setUsername(PreferencesStore.get(
                    SshToolsApplication.PREF_CONNECTION_LAST_USER, ""));
        }

        final SshToolsConnectionPanel conx = new SshToolsConnectionPanel(true);

        if (optionalTabs != null) {
            for (int i = 0; i < optionalTabs.length; i++) {
                conx.addTab(optionalTabs[i]);
            }
        }

        conx.setConnectionProfile(profile);

        JDialog d = null;
        Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class,
                parent);

        if (w instanceof JDialog) {
            d = new JDialog((JDialog) w, "Connection Profile", true);
        } else if (w instanceof JFrame) {
            d = new JDialog((JFrame) w, "Connection Profile", true);
        } else {
            d = new JDialog((JFrame) null, "Connection Profile", true);
        }

        final JDialog dialog = d;

        class UserAction {
            boolean connect;
        }

        final UserAction userAction = new UserAction();

        //  Create the bottom button panel
        final JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('c');
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    dialog.setVisible(false);
                }
            });

        final JButton connect = new JButton("Connect");
        connect.setMnemonic('t');
        connect.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (conx.validateTabs()) {
                        userAction.connect = true;
                        dialog.setVisible(false);
                    }
                }
            });
        dialog.getRootPane().setDefaultButton(connect);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(6, 6, 0, 0);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(buttonPanel, connect, gbc,
            GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(buttonPanel, cancel, gbc,
            GridBagConstraints.REMAINDER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.add(buttonPanel);

        //
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(conx, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Show the dialog
        dialog.getContentPane().setLayout(new GridLayout(1, 1));
        dialog.getContentPane().add(mainPanel);
        dialog.pack();
        dialog.setResizable(false);
        UIUtil.positionComponent(SwingConstants.CENTER, dialog);
        dialog.setVisible(true);

        if (!userAction.connect) {
            return null;
        }

        conx.applyTabs();

        // Make sure we didn't cancel
        PreferencesStore.put(SshToolsApplication.PREF_CONNECTION_LAST_HOST,
            profile.getHost());
        PreferencesStore.put(SshToolsApplication.PREF_CONNECTION_LAST_USER,
            profile.getUsername());
        PreferencesStore.putInt(SshToolsApplication.PREF_CONNECTION_LAST_PORT,
            profile.getPort());

        // Return the connection properties
        return profile;
    }
}
