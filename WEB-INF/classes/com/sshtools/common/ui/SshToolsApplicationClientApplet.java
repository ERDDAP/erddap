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
import com.sshtools.common.util.PropertyUtil;

import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClientFactory;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.io.IOUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public abstract class SshToolsApplicationClientApplet
    extends SshToolsApplicationApplet {
    /**  */
    public final static String[][] CLIENT_PARAMETER_INFO = {
        {
            "sshapps.connection.url", "string",
            "The URL of a connection profile to open"
        },
        { "sshapps.connection.host", "string", "The host to connect to" },
        { "sshapps.connection.userName", "string", "The user to connect as" },
        {
            "sshapps.connection.authenticationMethod", "string",
            "Authentication method. password,publickey etc."
        },
        {
            "sshapps.connection.connectImmediately", "boolean",
            "Connect immediately."
        },
        {
            "sshapps.connection.showConnectionDialog", "boolean",
            "Show connection dialog."
        },
        {
            "sshapps.connection.disableHostKeyVerification", "boolean",
            "Disable the host key verification dialog."
        }
    };

    /**  */
    protected Log log = LogFactory.getLog(SshToolsApplicationClientApplet.class);

    //  Private instance variables
    private String connectionProfileLocation;

    /**  */
    protected String authenticationMethod;

    /**  */
    protected String host;

    /**  */
    protected int port;

    /**  */
    protected String user;

    /**  */
    protected boolean connectImmediately;

    /**  */
    protected boolean showConnectionDialog;

    /**  */
    protected boolean disableHostKeyVerification;

    /**  */
    protected SshToolsConnectionProfile profile;

    /**
*
*
* @throws IOException
*/
    public void initApplet() throws IOException {
        super.initApplet();
        connectionProfileLocation = getParameter("sshapps.connectionProfile.url",
                "");

        //   Get the connection parameters
        host = getParameter("sshapps.connection.host", "");
        port = PropertyUtil.stringToInt(getParameter(
                    "sshapps.connection.port", ""), 22);
        user = getParameter("sshapps.connection.userName",
                ConfigurationLoader.checkAndGetProperty("user.home", ""));
        authenticationMethod = getParameter("sshapps.connection.authenticationMethod",
                "");
        connectImmediately = getParameter("sshapps.connection.connectImmediately",
                "false").equalsIgnoreCase("true");
        showConnectionDialog = getParameter("sshapps.connection.showConnectionDialog",
                "false").equalsIgnoreCase("true");
        disableHostKeyVerification = getParameter("sshapps.connection.disableHostKeyVerification",
                "false").equalsIgnoreCase("true");
        buildProfile();
    }

    /**
*
*/
    public void startApplet() {
        // Disable the host key verification if requested
        if (disableHostKeyVerification) {
            ((SshToolsApplicationClientPanel) applicationPanel).setHostHostVerification(null);
            ((SshToolsApplicationClientPanel) applicationPanel).application.removeAdditionalOptionsTab(
                "Hosts");
            log.debug("Host key verification disabled");
        } else {
            log.debug("Host key verification enabled");
        }

        if (connectImmediately) {
            loadingPanel.setStatus("Connecting");

            if (showConnectionDialog) {
                SshToolsConnectionProfile newProfile = ((SshToolsApplicationClientPanel) applicationPanel).newConnectionProfile(profile);

                if (newProfile != null) {
                    profile = newProfile;
                    ((SshToolsApplicationClientPanel) applicationPanel).connect(profile,
                        true);
                }
            } else {
                ((SshToolsApplicationClientPanel) applicationPanel).connect(profile,
                    false);
            }
        }
    }

    /**
*
*
* @throws IOException
*/
    protected void buildProfile() throws IOException {
        profile = new SshToolsConnectionProfile();

        //  Load the connection profile if specified
        if (!connectionProfileLocation.equals("")) {
            log.info("Loading connection profile " + connectionProfileLocation);
            loadingPanel.setStatus("Loading connection profile");

            InputStream in = null;

            try {
                URL u = null;

                try {
                    u = new URL(connectionProfileLocation);
                } catch (MalformedURLException murle) {
                    u = new URL(getCodeBase() + "/" +
                            connectionProfileLocation);
                }

                log.info("Full URL of connection profile is " + u);
                in = u.openStream();
                profile.open(in);
            } finally {
                IOUtil.closeStream(in);
            }
        }

        if (!host.equals("")) {
            log.info("Building connection profile from parameters ");
            log.debug("Setting host to " + host);
            profile.setHost(host);
            log.debug("Setting port to " + port);
            profile.setPort(port);
            log.debug("Setting username to " + user);
            profile.setUsername(user);

            if (!authenticationMethod.equals("")) {
                try {
                    log.debug("Adding authentication method " +
                        authenticationMethod);

                    SshAuthenticationClient authClient = SshAuthenticationClientFactory.newInstance(authenticationMethod);
                    profile.addAuthenticationMethod(authClient);
                } catch (Exception e) {
                    log.error("Could not add authentication method.", e);
                }
            }
        }
    }

    /**
*
*/
    public void destroy() {
        if (applicationPanel.isConnected()) {
            ((SshToolsApplicationClientPanel) applicationPanel).closeConnection(true);
        }
    }

    /**
*
*
* @return
*/
    public String[][] getParameterInfo() {
        String[][] s = super.getParameterInfo();
        String[][] p = new String[s.length + CLIENT_PARAMETER_INFO.length][];
        System.arraycopy(s, 0, p, 0, s.length);
        System.arraycopy(CLIENT_PARAMETER_INFO, 0, p, s.length,
            CLIENT_PARAMETER_INFO.length);

        return p;
    }
}
