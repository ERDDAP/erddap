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

import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClientFactory;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.19 $
 */
public class SshToolsConnectionHostTab extends JPanel
    implements SshToolsConnectionTab {
    //

    /**  */
    public final static int DEFAULT_PORT = 22;

    //

    /**  */
    public final static String CONNECT_ICON = "largeserveridentity.png";

    /**  */
    public final static String AUTH_ICON = "largelock.png";

    /**  */
    public final static String SHOW_AVAILABLE = "<Show available methods>";

    //

    /**  */
    protected XTextField jTextHostname = new XTextField();

    /**  */
    protected NumericTextField jTextPort = new NumericTextField(new Integer(0),
            new Integer(65535), new Integer(DEFAULT_PORT));

    /**  */
    protected XTextField jTextUsername = new XTextField();

    /**  */
    protected JList jListAuths = new JList();

    /**  */
    protected java.util.List methods = new ArrayList();

    /**  */
    protected SshToolsConnectionProfile profile;

    /**  */
    protected JCheckBox allowAgentForwarding;

    /**  */
    protected Log log = LogFactory.getLog(SshToolsConnectionHostTab.class);

    /**
* Creates a new SshToolsConnectionHostTab object.
*/
    public SshToolsConnectionHostTab() {
        super();

        //  Create the main connection details panel
        JPanel mainConnectionDetailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 2, 2, 2);
        gbc.weightx = 1.0;

        //  Host name
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel, new JLabel("Hostname"),
            gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel, jTextHostname, gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

        //  Port
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel, new JLabel("Port"), gbc,
            GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel, jTextPort, gbc,
            GridBagConstraints.REMAINDER);

        //  Username
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel, new JLabel("Username"),
            gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel, jTextUsername, gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

        //
        IconWrapperPanel iconMainConnectionDetailsPanel = new IconWrapperPanel(new ResourceIcon(
                    SshToolsConnectionHostTab.class, CONNECT_ICON),
                mainConnectionDetailsPanel);

        //  Authentication methods panel
        JPanel authMethodsPanel = new JPanel(new GridBagLayout());
        authMethodsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;

        //  Authentication methods
        UIUtil.jGridBagAdd(authMethodsPanel,
            new JLabel("Authentication Methods"), gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        jListAuths.setVisibleRowCount(5);
        UIUtil.jGridBagAdd(authMethodsPanel, new JScrollPane(jListAuths), gbc,
            GridBagConstraints.REMAINDER);
        allowAgentForwarding = new JCheckBox("Allow agent forwarding");
        UIUtil.jGridBagAdd(authMethodsPanel, allowAgentForwarding, gbc,
            GridBagConstraints.REMAINDER);

        //
        IconWrapperPanel iconAuthMethodsPanel = new IconWrapperPanel(new ResourceIcon(
                    SshToolsConnectionHostTab.class, AUTH_ICON),
                authMethodsPanel);

        //  This panel
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;
        UIUtil.jGridBagAdd(this, iconMainConnectionDetailsPanel, gbc,
            GridBagConstraints.REMAINDER);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(this, iconAuthMethodsPanel, gbc,
            GridBagConstraints.REMAINDER);

        //  Set up the values in the various components
        addAuthenticationMethods();
    }

    /**
*
*
* @param profile
*/
    public void setConnectionProfile(SshToolsConnectionProfile profile) {
        this.profile = profile;
        jTextHostname.setText((profile == null) ? "" : profile.getHost());
        jTextUsername.setText((profile == null) ? "" : profile.getUsername());
        jTextPort.setValue(new Integer((profile == null) ? 22 : profile.getPort()));

        if (System.getProperty("sshtools.agent") == null) {
            allowAgentForwarding.setSelected(false);
            allowAgentForwarding.setEnabled(false);
        } else {
            allowAgentForwarding.setEnabled(true);
            allowAgentForwarding.setSelected((profile != null) &&
                profile.getAllowAgentForwarding());
        }

        // Match the authentication methods
        Map auths = (profile == null) ? new HashMap()
                                      : profile.getAuthenticationMethods();
        Iterator it = auths.entrySet().iterator();
        Map.Entry entry;
        String authmethod;
        int[] selectionarray = new int[auths.values().size()];
        int count = 0;
        ListModel model = jListAuths.getModel();

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            authmethod = (String) entry.getKey();

            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).equals(authmethod)) {
                    selectionarray[count++] = i;

                    break;
                }
            }

            /*if (jListAuths.getNextMatch(authmethod, 0, Position.Bias.Forward) > -1) {
selectionarray[count] = jListAuths.getNextMatch(authmethod, 0,
Position.Bias.Forward);
count++;
}*/
            jListAuths.clearSelection();
            jListAuths.setSelectedIndices(selectionarray);
        }
    }

    /**
*
*
* @return
*/
    public SshToolsConnectionProfile getConnectionProfile() {
        return profile;
    }

    private void addAuthenticationMethods() {
        java.util.List methods = new ArrayList();
        methods.add(SHOW_AVAILABLE);
        methods.addAll(SshAuthenticationClientFactory.getSupportedMethods());
        jListAuths.setListData(methods.toArray());
        jListAuths.setSelectedIndex(0);
    }

    /**
*
*
* @return
*/
    public String getTabContext() {
        return "Connection";
    }

    /**
*
*
* @return
*/
    public Icon getTabIcon() {
        return null;
    }

    /**
*
*
* @return
*/
    public String getTabTitle() {
        return "Host";
    }

    /**
*
*
* @return
*/
    public String getTabToolTipText() {
        return "The main host connection details.";
    }

    /**
*
*
* @return
*/
    public int getTabMnemonic() {
        return 'h';
    }

    /**
*
*
* @return
*/
    public Component getTabComponent() {
        return this;
    }

    /**
*
*
* @return
*/
    public boolean validateTab() {
        // Validate that we have enough information
        if (jTextHostname.getText().equals("") ||
                jTextPort.getText().equals("") ||
                jTextUsername.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter all details!",
                "Connect", JOptionPane.OK_OPTION);

            return false;
        }

        // Setup the authentications selected
        java.util.List chosen = getChosenAuth();

        if (chosen != null) {
            Iterator it = chosen.iterator();

            while (it.hasNext()) {
                String method = (String) it.next();

                try {
                    SshAuthenticationClient auth = SshAuthenticationClientFactory.newInstance(method);
                } catch (AlgorithmNotSupportedException anse) {
                    JOptionPane.showMessageDialog(this,
                        method + " is not supported!");

                    return false;
                }
            }
        }

        return true;
    }

    private java.util.List getChosenAuth() {
        // Determine whether any authenticaiton methods we selected
        Object[] auths = jListAuths.getSelectedValues();
        String a;
        java.util.List l = new java.util.ArrayList();

        if (auths != null) {
            for (int i = 0; i < auths.length; i++) {
                a = (String) auths[i];

                if (a.equals(SHOW_AVAILABLE)) {
                    return null;
                } else {
                    l.add(a);
                }
            }
        } else {
            return null;
        }

        return l;
    }

    /**
*
*/
    public void applyTab() {
        profile.setHost(jTextHostname.getText());
        profile.setPort(Integer.valueOf(jTextPort.getText()).intValue());
        profile.setUsername(jTextUsername.getText());
        profile.setAllowAgentForwarding(allowAgentForwarding.getModel()
                                                            .isSelected());

        java.util.List chosen = getChosenAuth();

        // Remove the authentication methods and re-apply them
        profile.removeAuthenticationMethods();

        if (chosen != null) {
            Iterator it = chosen.iterator();

            while (it.hasNext()) {
                String method = (String) it.next();

                try {
                    SshAuthenticationClient auth = SshAuthenticationClientFactory.newInstance(method);
                    auth.setUsername(jTextUsername.getText());
                    profile.addAuthenticationMethod(auth);
                } catch (AlgorithmNotSupportedException anse) {
                    log.error("This should have been caught by validateTab()?",
                        anse);
                }
            }
        }
    }

    /**
*
*/
    public void tabSelected() {
    }
}
