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

import com.sshtools.common.configuration.Authorization;

import com.sshtools.j2ssh.transport.publickey.InvalidSshKeyException;
import com.sshtools.j2ssh.transport.publickey.SECSHPublicKeyFormat;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sshtools.j2ssh.transport.publickey.SshPublicKeyFile;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class SshtoolsAuthorizedKeysFormat implements AuthorizedKeysFormat {
    /**
*
*
* @param keys
*
* @return
*
* @throws java.lang.UnsupportedOperationException
*/
    public byte[] format(AuthorizedKeys keys) {
        throw new java.lang.UnsupportedOperationException(
            "SSHTools authorized keys format requries seperate key files!");
    }

    /**
*
*
* @param formatted
*
* @return
*
* @throws java.lang.UnsupportedOperationException
*/
    public AuthorizedKeys unformat(byte[] formatted) {
        throw new java.lang.UnsupportedOperationException(
            "SSHTools authorized keys format requries seperate key files!");
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
        Authorization authorization = new Authorization();
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
            authorization.addKey(filename);
        }

        return authorization.toString().getBytes("US-ASCII");
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
        try {
            AuthorizedKeys keys = new AuthorizedKeys();
            Authorization authorization = new Authorization(new ByteArrayInputStream(
                        formatted));
            List keyfiles = authorization.getAuthorizedKeys();
            Iterator it = keyfiles.iterator();
            String filename;
            SshPublicKeyFile pubfile;
            String username;

            while (it.hasNext()) {
                filename = (String) it.next();
                pubfile = SshPublicKeyFile.parse(loader.loadFile(filename));
                username = filename.substring(0, filename.length() - 4);
                keys.addKey(username, pubfile.toPublicKey());
            }

            return keys;
        } catch (ParserConfigurationException ex) {
            throw new IOException("Failed to read authorization file: " +
                ex.getMessage());
        } catch (SAXException ex) {
            throw new IOException("Failed to read authorization file: " +
                ex.getMessage());
        }
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
