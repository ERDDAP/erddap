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
package com.sshtools.daemon.platform;

import com.sshtools.daemon.configuration.PlatformConfiguration;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Map;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public abstract class NativeProcessProvider { //implements SessionDataProvider {

    private static Log log = LogFactory.getLog(NativeProcessProvider.class);
    private static Class provider;

    static {
        try {
            if (ConfigurationLoader.isConfigurationAvailable(
                        PlatformConfiguration.class)) {
                provider = ConfigurationLoader.getExtensionClass(((PlatformConfiguration) ConfigurationLoader.getConfiguration(
                            PlatformConfiguration.class)).getNativeProcessProvider());
            }
        } catch (Exception e) {
            log.error("Failed to load native process provider", e);
            provider = null;
        }
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public static NativeProcessProvider newInstance() throws IOException {
        try {
            return (NativeProcessProvider) provider.newInstance();
        } catch (Exception e) {
            throw new IOException(
                "The process provider failed to create a new instance: " +
                e.getMessage());
        }
    }

    /**
 *
 *
 * @param provider
 */
    public static void setProvider(Class provider) {
        NativeProcessProvider.provider = provider;
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public abstract InputStream getInputStream() throws IOException;

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public abstract OutputStream getOutputStream() throws IOException;

    /**
 *
 *
 * @return
 */
    public abstract InputStream getStderrInputStream()
        throws IOException;

    /**
 *
 */
    public abstract void kill();

    /**
 *
 *
 * @return
 */
    public abstract boolean stillActive();

    /**
 *
 *
 * @return
 */
    public abstract int waitForExitCode();

    /**
 *
 *
 * @return
 */
    public abstract String getDefaultTerminalProvider();

    /**
 *
 *
 * @param command
 * @param environment
 *
 * @return
 *
 * @throws IOException
 */
    public abstract boolean createProcess(String command, Map environment)
        throws IOException;

    /**
 *
 *
 * @throws IOException
 */
    public abstract void start() throws IOException;

    /**
 *
 *
 * @param term
 *
 * @return
 */
    public abstract boolean supportsPseudoTerminal(String term);

    /**
 *
 *
 * @param term
 * @param cols
 * @param rows
 * @param width
 * @param height
 * @param modes
 *
 * @return
 */
    public abstract boolean allocatePseudoTerminal(String term, int cols,
        int rows, int width, int height, String modes);
}
