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
package com.sshtools.common.hosts;

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

import java.awt.Component;

import java.io.File;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class DialogKnownHostsKeyVerification
    extends AbstractKnownHostsKeyVerification {
    Component parent;
    private boolean verificationEnabled = true;

    /**
* Creates a new DialogKnownHostsKeyVerification object.
*
* @param parent
*
* @throws InvalidHostFileException
*/
    public DialogKnownHostsKeyVerification(Component parent)
        throws InvalidHostFileException {
        super(new File(System.getProperty("user.home"),
                ".ssh" + File.separator + "known_hosts").getAbsolutePath());
        this.parent = parent;
    }

    /**
* Creates a new DialogKnownHostsKeyVerification object.
*
* @param parent
* @param hostFileName
*
* @throws InvalidHostFileException
*/
    public DialogKnownHostsKeyVerification(Component parent, String hostFileName)
        throws InvalidHostFileException {
        super(hostFileName);
        this.parent = parent;
    }

    /**
*
*
* @param enabled
*/
    public void setVerificationEnabled(boolean enabled) {
        this.verificationEnabled = verificationEnabled;
    }

    /**
*
*
* @param host
*
* @throws TransportProtocolException
*/
    public void onDeniedHost(final String host)
        throws TransportProtocolException {
        // Show a message to the user to inform them that the host
        // is denied
        try {
            if (verificationEnabled) {
                SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(parent,
                                "Access to '" + host + "' is denied.\n" +
                                "Verify the access granted/denied in the allowed hosts file.",
                                "Remote Host Authentication",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    });
            }
        } catch (InvocationTargetException ite) {
            throw new TransportProtocolException("Invocation Exception: " +
                ite.getMessage());
        } catch (InterruptedException ie) {
            throw new TransportProtocolException(
                "SwingUtilities thread interrupted!");
        }
    }

    /**
*
*
* @param host
* @param recorded
* @param actual
*
* @throws TransportProtocolException
*/
    public void onHostKeyMismatch(final String host,
        final SshPublicKey recorded, final SshPublicKey actual)
        throws TransportProtocolException {
        try {
            if (verificationEnabled) {
                SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            Object[] options = getOptions();
                            int res = JOptionPane.showOptionDialog(parent,
                                    "The host '" + host +
                                    "' has provided a different host key.\nThe host key" +
                                    " fingerprint provided is '" +
                                    actual.getFingerprint() + "'.\n" +
                                    "The allowed host key fingerprint is " +
                                    recorded.getFingerprint() +
                                    ".\nDo you want to allow this host?",
                                    "Remote host authentication",
                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null,
                                    options, options[0]);

                            try {
                                // Handle the reply
                                if ((options.length == 3) && (res == 0)) {
                                    // Always allow the host with the new fingerprint
                                    allowHost(host, actual, true);
                                } else if (((options.length == 2) &&
                                        (res == 0)) ||
                                        ((options.length == 3) && (res == 1))) {
                                    // Only allow the host this once
                                    allowHost(host, actual, false);
                                }
                            } catch (InvalidHostFileException e) {
                                showExceptionMessage(e);
                            }
                        }
                    });
            }
        } catch (InvocationTargetException ite) {
            throw new TransportProtocolException("Invocation Exception: " +
                ite.getMessage());
        } catch (InterruptedException ie) {
            throw new TransportProtocolException(
                "SwingUtilities thread interrupted!");
        }
    }

    /**
*
*
* @param host
* @param key
*
* @throws TransportProtocolException
*/
    public void onUnknownHost(final String host, final SshPublicKey key)
        throws TransportProtocolException {
        // Set up the users options. Only allow always if we can
        // write to the hosts file
        try {
            if (verificationEnabled) {
                SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            Object[] options = getOptions();
                            int res = JOptionPane.showOptionDialog(parent,
                                    "The host '" + host +
                                    "' is unknown. The host key" +
                                    " fingerprint is\n'" +
                                    key.getFingerprint() +
                                    "'.\nDo you want to allow this host?",
                                    "Remote host authentication",
                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null,
                                    options, options[0]);

                            try {
                                // Handle the reply
                                if ((options.length == 3) && (res == 0)) {
                                    // Always allow the host with the new fingerprint
                                    allowHost(host, key, true);
                                } else if (((options.length == 2) &&
                                        (res == 0)) ||
                                        ((options.length == 3) && (res == 1))) {
                                    // Only allow the host this once
                                    allowHost(host, key, false);
                                }
                            } catch (InvalidHostFileException e) {
                                showExceptionMessage(e);
                            }
                        }
                    });
            }
        } catch (InvocationTargetException ite) {
            throw new TransportProtocolException("Invocation Exception: " +
                ite.getMessage());
        } catch (InterruptedException ie) {
            throw new TransportProtocolException(
                "SwingUtilities thread interrupted!");
        }
    }

    private String[] getOptions() {
        return isHostFileWriteable() ? new String[] { "Always", "Yes", "No" }
                                     : new String[] { "Yes", "No" };
    }

    private void showExceptionMessage(Exception e) {
        JOptionPane.showMessageDialog(parent,
            "An unexpected error occured!\n\n" + e.getMessage(),
            "Host Verification", JOptionPane.ERROR_MESSAGE);
    }
}
