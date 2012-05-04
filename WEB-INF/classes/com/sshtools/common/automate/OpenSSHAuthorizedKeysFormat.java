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
import com.sshtools.j2ssh.transport.publickey.OpenSSHPublicKeyFormat;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sshtools.j2ssh.transport.publickey.SshPublicKeyFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Iterator;
import java.util.Map;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class OpenSSHAuthorizedKeysFormat implements AuthorizedKeysFormat {
    /**
*
*
* @param keys
* @param saver
*
* @return
*
* @throws IOException
* @throws InvalidSshKeyException
* @throws UnsupportedOperationException
*/
    public byte[] format(AuthorizedKeys keys, AuthorizedKeysFileSaver saver)
        throws IOException, InvalidSshKeyException {
        throw new UnsupportedOperationException(
            "The OpenSSH authorized key file does not support additional key files!");
    }

    /**
*
*
* @param formatted
* @param loader
*
* @return
*
* @throws IOException
* @throws InvalidSshKeyException
* @throws UnsupportedOperationException
*/
    public AuthorizedKeys unformat(byte[] formatted,
        AuthorizedKeysFileLoader loader)
        throws IOException, InvalidSshKeyException {
        throw new UnsupportedOperationException(
            "The OpenSSH authorized key file does not support additional key files!");
    }

    /**
*
*
* @param keys
*
* @return
*
* @throws IOException
* @throws InvalidSshKeyException
*/
    public byte[] format(AuthorizedKeys keys)
        throws IOException, InvalidSshKeyException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SshPublicKeyFile pubfile;
        OpenSSHPublicKeyFormat openssh = new OpenSSHPublicKeyFormat();
        Map.Entry entry;

        for (Iterator it = keys.getAuthorizedKeys().entrySet().iterator();
                (it != null) && it.hasNext();) {
            entry = (Map.Entry) it.next();
            openssh.setComment((String) entry.getValue());
            pubfile = SshPublicKeyFile.create((SshPublicKey) entry.getKey(),
                    openssh);
            out.write(pubfile.toString().getBytes("US-ASCII"));
            out.write('\n');
        }

        return out.toByteArray();
    }

    /**
*
*
* @param formatted
*
* @return
*
* @throws IOException
* @throws InvalidSshKeyException
*/
    public AuthorizedKeys unformat(byte[] formatted)
        throws IOException, InvalidSshKeyException {
        AuthorizedKeys keys = new AuthorizedKeys();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(formatted)));
        String line;
        SshPublicKeyFile pubfile;

        while ((line = reader.readLine()) != null) {
            pubfile = SshPublicKeyFile.parse(line.getBytes("US-ASCII"));
            keys.addKey(pubfile.getComment(), pubfile.toPublicKey());
        }

        return keys;
    }

    /**
*
*
* @return
*/
    public boolean requiresKeyFiles() {
        return false;
    }
}
