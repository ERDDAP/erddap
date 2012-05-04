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

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.19 $
 */
public class RemoteIdentification {
    /**  */
    public static final int ADD_AUTHORIZEDKEY = 1;

    /**  */
    public static final int REMOVE_AUTHORIZEDKEY = 2;
    private String defaultName;
    private Vector rules = new Vector();
    private Class authorizedKeysFormat;
    private String defaultPath;

    /**  */
    protected Log log = LogFactory.getLog(RemoteIdentification.class);

    /**
* Creates a new RemoteIdentification object.
*
* @param defaultName
*/
    public RemoteIdentification(String defaultName) {
        this.defaultName = defaultName;
    }

    /**
*
*
* @return
*/
    protected List getRules() {
        return rules;
    }

    /**
*
*
* @return
*/
    public String getDefaultName() {
        return defaultName;
    }

    /**
*
*
* @param ident
*
* @return
*
* @throws RemoteIdentificationException
*/
    public String getName(String ident) throws RemoteIdentificationException {
        boolean pass = false;
        Iterator it = rules.iterator();
        Vector passed = new Vector();
        RemoteIdentificationRule rule;
        String rulename = null;

        // Check all the rules
        while (it.hasNext()) {
            rule = (RemoteIdentificationRule) it.next();

            if (rule.testRule(ident)) {
                passed.add(rule);
            }
        }

        if (passed.size() > 0) {
            // Select the highest priority rule where 0=highest 10=lowest
            it = passed.iterator();

            RemoteIdentificationRule ret = null;

            while (it.hasNext()) {
                rule = (RemoteIdentificationRule) it.next();

                if (ret == null) {
                    ret = rule;
                } else {
                    if (rule.getPriority() < ret.getPriority()) {
                        ret = rule;
                    }
                }
            }

            if (ret.getName() != null) {
                return ret.getName();
            } else {
                return defaultName;
            }
        } else {
            throw new RemoteIdentificationException(
                "No rules exist to identify the remote host with ident string " +
                ident);
        }
    }

    /**
*
*
* @param rule
*/
    protected void addRule(RemoteIdentificationRule rule) {
        rules.add(rule);
    }

    /**
*
*
* @param ident
*
* @return
*/
    protected boolean testRules(String ident) {
        boolean pass = false;
        Iterator it = rules.iterator();
        RemoteIdentificationRule rule;

        while (it.hasNext() && !pass) {
            rule = (RemoteIdentificationRule) it.next();
            pass = rule.testRule(ident);
        }

        return pass;
    }

    /**
*
*
* @param implementationClass
*/
    protected void setAuthorizedKeysFormat(Class implementationClass) {
        authorizedKeysFormat = implementationClass;
    }

    /**
*
*
* @param defaultPath
*/
    protected void setAuthorizedKeysDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    /**
*
*
* @return
*/
    public String getAuthorizedKeysDefaultPath() {
        return defaultPath;
    }

    /**
*
*
* @return
*
* @throws RemoteIdentificationException
*/
    public AuthorizedKeysFormat getAuthorizedKeysFormat()
        throws RemoteIdentificationException {
        try {
            if (authorizedKeysFormat != null) {
                return (AuthorizedKeysFormat) authorizedKeysFormat.newInstance();
            } else {
                throw new RemoteIdentificationException(
                    "There is no authorized keys format set for this remote id");
            }
        } catch (Exception ex) {
            throw new RemoteIdentificationException("Failed to instansiate " +
                authorizedKeysFormat.getName());
        }
    }

    /**
*
*
* @param sftp
* @param serverId
* @param system
* @param username
* @param pk
* @param authorizationFile
* @param mode
*
* @return
*
* @throws RemoteIdentificationException
*/
    public boolean configureUserAccess(SftpClient sftp, String serverId,
        String system, String username, SshPublicKey pk,
        String authorizationFile, int mode)
        throws RemoteIdentificationException {
        Vector keys = new Vector();
        keys.add(pk);

        return configureUserAccess(sftp, serverId, system, username, keys,
            authorizationFile, mode);
    }

    /**
*
*
* @param sftp
* @param serverId
* @param system
* @param username
* @param keys
* @param authorizationFile
* @param mode
*
* @return
*
* @throws RemoteIdentificationException
*/
    public boolean configureUserAccess(final SftpClient sftp,
        final String serverId, String system, String username, List keys,
        String authorizationFile, int mode)
        throws RemoteIdentificationException {
        try {
            if (sftp.isClosed()) {
                throw new RemoteIdentificationException(
                    "SFTP connection must be open");
            }

            if (authorizationFile == null) {
                throw new RemoteIdentificationException(
                    "authorization file cannot be null");
            }

            if ((mode != ADD_AUTHORIZEDKEY) && (mode != REMOVE_AUTHORIZEDKEY)) {
                throw new RemoteIdentificationException(
                    "Invalid configuration mode specifed in call to configureUserAccess");
            }

            AuthorizedKeys authorizedKeys;
            authorizationFile.replace('\\', '/');

            final String directory = ((authorizationFile.lastIndexOf("/") > 0)
                ? authorizationFile.substring(0,
                    authorizationFile.lastIndexOf("/") + 1) : "");

            try {
                // Remove the old backup - ignore the error
                try {
                    sftp.rm(authorizationFile + ".bak");
                } catch (IOException ex) {
                }

                // Change the current authorization file to the backup
                sftp.rename(authorizationFile, authorizationFile + ".bak");
                log.info("Opening existing authorized keys file from " +
                    authorizationFile + ".bak");

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                sftp.get(authorizationFile + ".bak", out);

                byte[] backup = out.toByteArray();
                out.close();

                // Obtain the current authoized keys settings
                log.info("Parsing authorized keys file");
                authorizedKeys = AuthorizedKeys.parse(backup, serverId, system,
                        new AuthorizedKeysFileLoader() {
                            public byte[] loadFile(String filename)
                                throws IOException {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                sftp.get(directory + filename, out);
                                out.close();

                                return out.toByteArray();
                            }
                        });
            } catch (IOException ioe) {
                // Could not open so create a new file
                authorizedKeys = new AuthorizedKeys();
            } catch (RemoteIdentificationException rie) {
                throw new RemoteIdentificationException(
                    "Open3SP cannot identify the remote host.\n" +
                    "Please email support@open3sp.org with specifying 'remote identification' in the subject and supplying the server type and the follwing data '" +
                    serverId + "'");
            }

            log.info("Updating authorized keys file");

            // Check the existing keys and add any that are not present
            SshPublicKey pk;

            for (Iterator x = keys.iterator(); x.hasNext();) {
                pk = (SshPublicKey) x.next();

                if (!authorizedKeys.containsKey(pk) &&
                        (mode == ADD_AUTHORIZEDKEY)) {
                    authorizedKeys.addKey(username, pk);
                } else if (authorizedKeys.containsKey(pk) &&
                        (mode == REMOVE_AUTHORIZEDKEY)) {
                    authorizedKeys.removeKey(pk);
                }
            }

            // Verfiy that the directory exists?
            log.info("Verifying directory " + directory);

            int umask = sftp.umask(0022);
            sftp.mkdirs(directory);

            // Output the new file
            log.info("Writing new authorized keys file to " +
                authorizationFile);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Output the authorization file to a ByteArrayOutputStream
            out.write(AuthorizedKeys.create(authorizedKeys, serverId, system,
                    new AuthorizedKeysFileSaver() {
                    public void saveFile(String filename, byte[] filedata)
                        throws IOException {
                        //SftpFile file = null;
                        ByteArrayInputStream in = null;

                        try {
                            in = new ByteArrayInputStream(filedata);
                            sftp.put(in, directory + filename);
                        } catch (IOException ex) {
                            log.info("Error writing public key file to server" +
                                    filename, ex);
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                        }
                    }
                }));
            out.close();

            // Copy the new authorisation file to the server
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            sftp.umask(0133);
            sftp.put(in, authorizationFile);
            sftp.umask(umask);

            return true;
        } catch (IOException ioe) {
            throw new RemoteIdentificationException(ioe.getMessage());
        } catch (RemoteIdentificationException rie) {
            throw new RemoteIdentificationException(
                "SSHTools cannot identify the remote host.\n" +
                "Please email support@sshtools.com specifying 'remote identification' in the subject, supplying the server type and the following data: '" +
                serverId + "'");
        }
    }
}
