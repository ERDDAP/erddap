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
package com.sshtools.common.automate;

import com.sshtools.j2ssh.configuration.ConfigurationException;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import java.util.Iterator;
import java.util.Map;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class RemoteIdentificationFactory {
    private static Map remoteIdentifications = null;

    static {
        try {
            if (ConfigurationLoader.isConfigurationAvailable(
                        AutomationConfiguration.class)) {
                remoteIdentifications = ((AutomationConfiguration) ConfigurationLoader.getConfiguration(AutomationConfiguration.class)).getRemoteIdentifications();
            }
        } catch (ConfigurationException ex) {
        }
    }

    /**
*
*
* @param serverId
*
* @return
*
* @throws RemoteIdentificationException
*/
    public static synchronized RemoteIdentification getInstance(
        String serverId, String hostname) throws RemoteIdentificationException {
        if (remoteIdentifications == null) {
            throw new RemoteIdentificationException(
                "There are no remote identification rules!");
        }

        Iterator it = remoteIdentifications.entrySet().iterator();
        Map.Entry entry;
        RemoteIdentification rid;

        // Check the hostname first
        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            rid = (RemoteIdentification) entry.getValue();

            if (hostname != null) {
                if (rid.getDefaultName().equals(hostname)) {
                    return rid;
                }
            }
        }

        it = remoteIdentifications.entrySet().iterator();

        // Now check against the rules
        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            rid = (RemoteIdentification) entry.getValue();

            if (rid.testRules(serverId)) {
                return rid;
            }
        }

        throw new RemoteIdentificationException(
            "Failed to find a remote identification rule");
    }

    /**
*
*
* @param args
*/
    public static void main(String[] args) {
        try {
            RemoteIdentification rid;
            String serverId = "http://www.sshtools.com J2SSH 0.1.1 beta [CLIENT]";
            rid = getInstance(serverId, null);
            System.out.println("Remote Identification: " +
                rid.getName(serverId));
            serverId = "OpenSSH_3.4p1";
            rid = getInstance(serverId, null);
            System.out.println("Remote Identification: " +
                rid.getName(serverId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
