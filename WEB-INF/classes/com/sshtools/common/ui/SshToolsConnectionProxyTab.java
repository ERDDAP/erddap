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
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class SshToolsConnectionProxyTab extends JPanel
    implements SshToolsConnectionTab {
    /**  */
    public final static String PROXY_ICON = "/com/sshtools/common/ui/proxy.png";

    /**  */
    protected JRadioButton noProxy = new JRadioButton("No proxy");

    /**  */
    protected JRadioButton httpProxy = new JRadioButton("HTTP");

    /**  */
    protected JRadioButton socks4Proxy = new JRadioButton("SOCKS 4");

    /**  */
    protected JRadioButton socks5Proxy = new JRadioButton("SOCKS 5");

    /**  */
    protected ButtonGroup group = new ButtonGroup();

    /**  */
    protected JPanel proxyframe = new JPanel(new GridBagLayout());

    /**  */
    protected JTextField username = new JTextField();

    /**  */
    protected JPasswordField password = new JPasswordField();

    /**  */
    protected JTextField proxy = new JTextField();

    /**  */
    protected NumericTextField port = new NumericTextField(new Integer(1),
            new Integer(65535));

    /**  */
    protected SshToolsConnectionProfile profile;

    /**  */
    protected Log log = LogFactory.getLog(SshToolsConnectionProxyTab.class);

    /**
* Creates a new SshToolsConnectionProxyTab object.
*/
    public SshToolsConnectionProxyTab() {
        super();
        group.add(noProxy);
        group.add(httpProxy);
        group.add(socks4Proxy);
        group.add(socks5Proxy);

        ChangeListener listener = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (noProxy.isSelected()) {
                        username.setEnabled(false);
                        password.setEnabled(false);
                        proxy.setEnabled(false);

                        //port.setEnabled(false);
                        port.setForeground(Color.white);
                    } else {
                        username.setEnabled(true);
                        password.setEnabled(true);
                        proxy.setEnabled(true);

                        //port.setEnabled(true);
                        port.setForeground(Color.black);

                        if (httpProxy.isSelected()) {
                            port.setText("80");
                        } else {
                            port.setText("1080");
                        }
                    }
                }
            };

        noProxy.getModel().addChangeListener(listener);
        httpProxy.getModel().addChangeListener(listener);
        socks4Proxy.getModel().addChangeListener(listener);
        socks5Proxy.getModel().addChangeListener(listener);

        //  Create the main connection details panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 2, 2, 2);
        gbc.weightx = 1.0;
        proxyframe.setBorder(BorderFactory.createTitledBorder(
                "Connect using the following proxy"));

        //  No proxy label
        gbc.insets = new Insets(2, 10, 2, 2);
        UIUtil.jGridBagAdd(proxyframe, noProxy, gbc, GridBagConstraints.RELATIVE);

        // Socks 4 label
        gbc.insets = new Insets(2, 15, 2, 2);
        UIUtil.jGridBagAdd(proxyframe, socks4Proxy, gbc,
            GridBagConstraints.REMAINDER);

        //gbc.fill = GridBagConstraints.HORIZONTAL;
        // Http Proxy
        gbc.insets = new Insets(2, 10, 2, 2);
        UIUtil.jGridBagAdd(proxyframe, httpProxy, gbc,
            GridBagConstraints.RELATIVE);

        // Socks 5 label
        gbc.insets = new Insets(2, 15, 2, 2);
        UIUtil.jGridBagAdd(proxyframe, socks5Proxy, gbc,
            GridBagConstraints.REMAINDER);
        gbc.insets = new Insets(2, 10, 2, 10);

        JPanel connect = new JPanel(new GridBagLayout());
        connect.setBorder(BorderFactory.createTitledBorder("Proxy Details"));
        UIUtil.jGridBagAdd(connect, new JLabel("Host"), gbc,
            GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(connect, proxy, gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(connect, new JLabel("Port"), gbc,
            GridBagConstraints.REMAINDER);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        UIUtil.jGridBagAdd(connect, port, gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(connect, new JLabel("Username"), gbc,
            GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(connect, username, gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(connect, new JLabel("Password"), gbc,
            GridBagConstraints.REMAINDER);
        gbc.insets = new Insets(2, 10, 10, 10);
        UIUtil.jGridBagAdd(connect, password, gbc, GridBagConstraints.REMAINDER);

        JPanel main = new JPanel(new GridBagLayout());
        gbc.insets = new Insets(2, 2, 2, 2);
        UIUtil.jGridBagAdd(main, proxyframe, gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(main, connect, gbc, GridBagConstraints.REMAINDER);

        IconWrapperPanel iconProxyDetailsPanel = new IconWrapperPanel(new ResourceIcon(
                    PROXY_ICON), main);
        noProxy.setSelected(true);

        //  This panel
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;
        add(iconProxyDetailsPanel, BorderLayout.NORTH);
    }

    /**
*
*
* @param profile
*/
    public void setConnectionProfile(SshToolsConnectionProfile profile) {
        this.profile = profile;

        if (profile.getTransportProvider() == SshToolsConnectionProfile.USE_HTTP_PROXY) {
            httpProxy.setSelected(true);
        } else if (profile.getTransportProvider() == SshToolsConnectionProfile.USE_SOCKS4_PROXY) {
            socks4Proxy.setSelected(true);
        } else if (profile.getTransportProvider() == SshToolsConnectionProfile.USE_SOCKS5_PROXY) {
            socks5Proxy.setSelected(true);
        }

        proxy.setText(profile.getProxyHost());

        if (profile.getProxyPort() > 0) {
            port.setValue(new Integer(profile.getProxyPort()));
        }

        username.setText(profile.getProxyUsername());
        password.setText(profile.getProxyPassword());
    }

    /**
*
*
* @return
*/
    public SshToolsConnectionProfile getConnectionProfile() {
        return profile;
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
        return "Proxy";
    }

    /**
*
*
* @return
*/
    public String getTabToolTipText() {
        return "Configure the proxy connection.";
    }

    /**
*
*
* @return
*/
    public int getTabMnemonic() {
        return 'p';
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
        return true;
    }

    /**
*
*/
    public void applyTab() {
        if (httpProxy.isSelected()) {
            profile.setTransportProvider(SshToolsConnectionProfile.USE_HTTP_PROXY);
        } else if (socks4Proxy.isSelected()) {
            profile.setTransportProvider(SshToolsConnectionProfile.USE_SOCKS4_PROXY);
        } else if (socks5Proxy.isSelected()) {
            profile.setTransportProvider(SshToolsConnectionProfile.USE_SOCKS5_PROXY);
        } else {
            profile.setTransportProvider(SshToolsConnectionProfile.USE_STANDARD_SOCKET);
        }

        profile.setProxyHost(proxy.getText());
        profile.setProxyPort(port.getValue().intValue());
        profile.setProxyUsername(username.getText());
        profile.setProxyPassword(new String(password.getPassword()));
    }

    /**
*
*/
    public void tabSelected() {
    }
}
