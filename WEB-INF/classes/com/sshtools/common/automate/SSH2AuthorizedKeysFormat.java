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
import com.sshtools.j2ssh.transport.publickey.SECSHPublicKeyFormat;
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
public class SSH2AuthorizedKeysFormat implements AuthorizedKeysFormat {
    private static final String header = "# Open3SP auto-generated authorization file\n";
    private static final String key = "key ";

    /**
*
*
* @param keys
*
* @return
*
* @throws IOException
* @throws InvalidSshKeyException
* @throws java.lang.UnsupportedOperationException
*/
    public byte[] format(AuthorizedKeys keys)
        throws IOException, InvalidSshKeyException {
        throw new java.lang.UnsupportedOperationException(
            "SSH2 authorized keys format requries seperate key files!");
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
* @throws java.lang.UnsupportedOperationException
*/
    public AuthorizedKeys unformat(byte[] formatted)
        throws IOException, InvalidSshKeyException {
        throw new java.lang.UnsupportedOperationException(
            "SSH2 authorized keys format requries seperate key files!");
    }

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
*/
    public byte[] format(AuthorizedKeys keys, AuthorizedKeysFileSaver saver)
        throws IOException, InvalidSshKeyException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.getBytes("US-ASCII"));

        SshPublicKeyFile pubfile;
        SECSHPublicKeyFormat secsh = new SECSHPublicKeyFormat();
        Map.Entry entry;

        for (Iterator it = keys.getAuthorizedKeys().entrySet().iterator();
                (it != null) && it.hasNext();) {
            entry = (Map.Entry) it.next();

            // Write out the public key file
            String username = (String) entry.getValue();
            String filename = username + ".pub";
            secsh.setComment(username);
            pubfile = SshPublicKeyFile.create((SshPublicKey) entry.getKey(),
                    secsh);
            saver.saveFile(filename, pubfile.toString().getBytes("US-ASCII"));

            // Write out the key entry
            out.write(key.getBytes("US-ASCII"));
            out.write(filename.getBytes("US-ASCII"));
            out.write('\n');
        }

        return out.toByteArray();
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
*/
    public AuthorizedKeys unformat(byte[] formatted,
        AuthorizedKeysFileLoader loader)
        throws IOException, InvalidSshKeyException {
        AuthorizedKeys keys = new AuthorizedKeys();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(formatted)));
        String line;
        SshPublicKeyFile pubfile;
        String filename;
        String username;

        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("key")) {
                // Get the filename and load
                filename = line.substring(line.trim().lastIndexOf(" ") + 1)
                               .trim();
                pubfile = SshPublicKeyFile.parse(loader.loadFile(filename));

                // Get the username from the filename - .pub
                username = filename.substring(0, filename.length() - 4);
                keys.addKey(username, pubfile.toPublicKey());
            }
        }

        return keys;
    }

    /**
*
*
* @return
*/
    public boolean requiresKeyFiles() {
        return true;
    }
}
