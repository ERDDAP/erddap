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

import com.sshtools.daemon.configuration.*;

import com.sshtools.j2ssh.configuration.*;

import org.apache.commons.logging.*;

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public abstract class NativeAuthenticationProvider {
    private static Log log = LogFactory.getLog(NativeAuthenticationProvider.class);
    private static Class cls;
    private static NativeAuthenticationProvider instance;

    static {
        try {
            if (ConfigurationLoader.isConfigurationAvailable(
                        PlatformConfiguration.class)) {
                cls = ConfigurationLoader.getExtensionClass(((PlatformConfiguration) ConfigurationLoader.getConfiguration(
                            PlatformConfiguration.class)).getNativeAuthenticationProvider());

                //
            }
        } catch (Exception e) {
            log.error("Failed to load native authentication provider", e);
            instance = null;
        }
    }

    /**
 *
 *
 * @param cls
 */
    public static void setProvider(Class cls) {
        NativeAuthenticationProvider.cls = cls;
    }

    /**
 *
 *
 * @param username
 *
 * @return
 *
 * @throws IOException
 */
    public abstract String getHomeDirectory(String username)
        throws IOException;

    /**
 *
 *
 * @param username
 * @param password
 *
 * @return
 *
 * @throws PasswordChangeException
 * @throws IOException
 */
    public abstract boolean logonUser(String username, String password)
        throws PasswordChangeException, IOException;

    /**
 *
 *
 * @param username
 *
 * @return
 *
 * @throws IOException
 */
    public abstract boolean logonUser(String username)
        throws IOException;

    /**
 *
 *
 * @throws IOException
 */
    public abstract void logoffUser() throws IOException;

    /**
 *
 *
 * @param username
 * @param oldpassword
 * @param newpassword
 *
 * @return
 */
    public abstract boolean changePassword(String username, String oldpassword,
        String newpassword);

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public static NativeAuthenticationProvider getInstance()
        throws IOException {
        if (instance == null) {
            try {
                if (cls == null) {
                    throw new IOException(
                        "There is no authentication provider configured");
                }

                instance = (NativeAuthenticationProvider) cls.newInstance();
            } catch (Exception e) {
                throw new IOException(
                    "The authentication provider failed to initialize: " +
                    e.getMessage());
            }
        }

        return instance;
    }
}
