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

import com.sshtools.j2ssh.transport.publickey.InvalidSshKeyException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class AuthorizedKeys {
    private HashMap keys = new HashMap();

    /**
*
*
* @return
*/
    public Map getAuthorizedKeys() {
        return keys;
    }

    /**
*
*
* @param username
* @param key
*/
    public void addKey(String username, SshPublicKey key) {
        if (!containsKey(key)) {
            keys.put(key, username);
        }
    }

    /**
*
*
* @param key
*/
    public void removeKey(SshPublicKey key) {
        keys.remove(key);
    }

    /**
*
*
* @param key
*
* @return
*/
    public boolean containsKey(SshPublicKey key) {
        return keys.containsValue(key);
    }

    /**
*
*
* @param formatted
* @param serverId
* @param loader
*
* @return
*
* @throws RemoteIdentificationException
* @throws IOException
* @throws InvalidSshKeyException
*/
    public static AuthorizedKeys parse(byte[] formatted, String serverId,
        String hostname, AuthorizedKeysFileLoader loader)
        throws RemoteIdentificationException, IOException, 
            InvalidSshKeyException {
        AuthorizedKeysFormat format = RemoteIdentificationFactory.getInstance(serverId,
                hostname).getAuthorizedKeysFormat();

        if (format.requiresKeyFiles()) {
            return format.unformat(formatted, loader);
        } else {
            return format.unformat(formatted);
        }
    }

    /**
*
*
* @param keys
* @param serverId
* @param saver
*
* @return
*
* @throws RemoteIdentificationException
* @throws IOException
* @throws InvalidSshKeyException
*/
    public static byte[] create(AuthorizedKeys keys, String serverId,
        String hostname, AuthorizedKeysFileSaver saver)
        throws RemoteIdentificationException, IOException, 
            InvalidSshKeyException {
        AuthorizedKeysFormat format = RemoteIdentificationFactory.getInstance(serverId,
                hostname).getAuthorizedKeysFormat();

        if (format.requiresKeyFiles()) {
            return format.format(keys, saver);
        } else {
            return format.format(keys);
        }
    }
}
