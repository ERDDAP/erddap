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
package com.sshtools.daemon;

import com.sshtools.common.configuration.*;

import com.sshtools.daemon.configuration.*;
import com.sshtools.daemon.forwarding.*;
import com.sshtools.daemon.session.*;

import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.connection.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.net.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.17 $
 */
public class SshDaemon {
    private static Log log = LogFactory.getLog(SshDaemon.class);

    /**
 *
 *
 * @param args
 */
    public static void main(String[] args) {
        try {
            XmlServerConfigurationContext context = new XmlServerConfigurationContext();
            context.setServerConfigurationResource(ConfigurationLoader.checkAndGetProperty(
                    "sshtools.server", "server.xml"));
            context.setPlatformConfigurationResource(System.getProperty(
                    "sshtools.platform", "platform.xml"));
            ConfigurationLoader.initialize(false, context);

            XmlConfigurationContext context2 = new XmlConfigurationContext();
            context2.setFailOnError(false);
            context2.setAPIConfigurationResource(ConfigurationLoader.checkAndGetProperty(
                    "sshtools.config", "sshtools.xml"));
            context2.setAutomationConfigurationResource(ConfigurationLoader.checkAndGetProperty(
                    "sshtools.automate", "automation.xml"));
            ConfigurationLoader.initialize(false, context2);

            if (args.length > 0) {
                if (args[0].equals("-start")) {
                    start();
                } else if (args[0].equals("-stop")) {
                    if (args.length > 1) {
                        stop(args[1]);
                    } else {
                        stop("The framework daemon is shutting down");
                    }
                } else {
                    System.out.println("Usage: SshDaemon [-start|-stop]");
                }
            } else {
                System.out.println("Usage: SshDaemon [-start|-stop]");
            }
        } catch (Exception e) {
            log.error("The server failed to process the " +
                ((args.length > 0) ? args[0] : "") + " command", e);
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public static void start() throws IOException {
        // We need at least one host key
        SshServer server = new SshServer() {
                public void configureServices(ConnectionProtocol connection)
                    throws IOException {
                    connection.addChannelFactory(SessionChannelFactory.SESSION_CHANNEL,
                        new SessionChannelFactory());

                    if (ConfigurationLoader.isConfigurationAvailable(
                                ServerConfiguration.class)) {
                        if (((ServerConfiguration) ConfigurationLoader.getConfiguration(
                                    ServerConfiguration.class)).getAllowTcpForwarding()) {
                            ForwardingServer forwarding = new ForwardingServer(connection);
                        }
                    }
                }

                public void shutdown(String msg) {
                    // Disconnect all sessions
                }
            };

        server.startServer();
    }

    /**
 *
 *
 * @param msg
 *
 * @throws IOException
 */
    public static void stop(String msg) throws IOException {
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(),
                    ((ServerConfiguration) ConfigurationLoader.getConfiguration(
                        ServerConfiguration.class)).getCommandPort());

            // Write the command id
            socket.getOutputStream().write(0x3a);

            // Write the length of the message (max 255)
            int len = (msg.length() <= 255) ? msg.length() : 255;
            socket.getOutputStream().write(len);

            // Write the message
            if (len > 0) {
                socket.getOutputStream().write(msg.substring(0, len).getBytes());
            }

            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
