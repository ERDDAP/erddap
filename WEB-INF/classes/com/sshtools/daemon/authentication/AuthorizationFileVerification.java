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
package com.sshtools.daemon.authentication;

import com.sshtools.common.configuration.*;

import com.sshtools.daemon.configuration.*;
import com.sshtools.daemon.platform.*;

import com.sshtools.j2ssh.authentication.*;
import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.transport.publickey.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class AuthorizationFileVerification implements PublicKeyVerification {
    private static Log log = LogFactory.getLog(AuthorizationFileVerification.class);

    /**
 *
 *
 * @param username
 * @param algorithm
 * @param encoded
 * @param service
 * @param sessionId
 * @param signature
 *
 * @return
 *
 * @throws IOException
 */
    public boolean verifyKeySignature(String username, String algorithm,
        byte[] encoded, String service, byte[] sessionId, byte[] signature)
        throws IOException {
        try {
            SshPublicKey key = getAuthorizedKey(username, algorithm, encoded);
            ByteArrayWriter data = new ByteArrayWriter();
            data.writeBinaryString(sessionId);
            data.write(SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST);
            data.writeString(username);
            data.writeString(service);
            data.writeString("publickey");
            data.write(1);
            data.writeString(key.getAlgorithmName());
            data.writeBinaryString(key.getEncoded());

            if (key.verifySignature(signature, data.toByteArray())) {
                return true;
            }
        } catch (IOException ex) {
        }

        return false;
    }

    private SshPublicKey getAuthorizedKey(String username, String algorithm,
        byte[] encoded) throws IOException {
        NativeAuthenticationProvider provider = NativeAuthenticationProvider.getInstance();
        String userHome = provider.getHomeDirectory(username); //, nativeSettings);

        if (userHome == null) {
            log.warn("There is no home directory for " + username +
                " is available");
        }

        // Replace '\' with '/' because when we use it in String.replaceAll
        // for some reason it removes them?
        if (userHome != null) {
            userHome = userHome.replace('\\', '/');
        }

        ServerConfiguration config = (ServerConfiguration) ConfigurationLoader.getConfiguration(ServerConfiguration.class);
        String authorizationFile;
        String userConfigDir = config.getUserConfigDirectory();

        // First replace any '\' with '/' (Becasue replaceAll removes them!)
        userConfigDir = userConfigDir.replace('\\', '/');

        // Replace any home directory tokens
        if ((userConfigDir.indexOf("%D") > -1) && (userHome == null)) {
            throw new IOException(
                "<UserConfigDirectory> requires home directory, but none available for " +
                username);
        }

        int idx = 0;

        while ((idx = userConfigDir.indexOf("%D", idx + 1)) > -1) {
            StringBuffer buf = new StringBuffer(userConfigDir);
            buf = buf.replace(idx, idx + 1, userHome);
            userConfigDir = buf.toString();
        }

        idx = 0;

        while ((idx = userConfigDir.indexOf("%U", idx + 1)) > -1) {
            StringBuffer buf = new StringBuffer(userConfigDir);
            buf = buf.replace(idx, idx + 1, username);
            userConfigDir = buf.toString();
        }

        // Replace the '/' with File.seperator and trim
        userConfigDir = userConfigDir.replace('/', File.separatorChar).trim();

        if (!userConfigDir.endsWith(File.separator)) {
            userConfigDir += File.separator;
        }

        authorizationFile = userConfigDir + config.getAuthorizationFile();

        // Load the authorization file
        File file = new File(authorizationFile);

        if (!file.exists()) {
            log.info("authorizationFile: " + authorizationFile +
                " does not exist.");
            throw new IOException("authorizationFile: " + authorizationFile +
                " does not exist.");
        }

        FileInputStream in = new FileInputStream(file);
        Authorization keys;

        try {
            keys = new Authorization(in);
        } catch (Exception e) {
            throw new AuthenticationProtocolException(
                "Failed to load authorized keys file " + authorizationFile);
        }

        //      SshPublicKey key = SshPublicKeyFile.parse(encoded);
        Iterator it = keys.getAuthorizedKeys().iterator();
        SshKeyPair pair = SshKeyPairFactory.newInstance(algorithm);
        SshPublicKey authorizedKey = null;
        SshPublicKey key = pair.decodePublicKey(encoded);
        boolean valid = false;
        String keyfile;

        while (it.hasNext()) {
            keyfile = (String) it.next();

            // Look for the file in the user config dir first
            file = new File(userConfigDir + keyfile);

            // If it does not exist then look absolute
            if (!file.exists()) {
                file = new File(keyfile);
            }

            if (file.exists()) {
                // Try to open the public key in the default file format
                // otherwise attempt the supported key formats
                SshPublicKeyFile pkf = SshPublicKeyFile.parse(file);
                authorizedKey = pkf.toPublicKey();

                if (authorizedKey.equals(key)) {
                    return authorizedKey;
                }
            } else {
                log.info("Failed attempt to load key file " + keyfile);
            }
        }

        throw new IOException("");
    }

    /**
 *
 *
 * @param username
 * @param algorithm
 * @param encoded
 *
 * @return
 *
 * @throws IOException
 */
    public boolean acceptKey(String username, String algorithm, byte[] encoded)
        throws IOException {
        try {
            getAuthorizedKey(username, algorithm, encoded);

            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
