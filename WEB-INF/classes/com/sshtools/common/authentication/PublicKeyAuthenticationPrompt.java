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
package com.sshtools.common.authentication;

import com.sshtools.j2ssh.authentication.AuthenticationProtocolException;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationPrompt;
import com.sshtools.j2ssh.transport.publickey.InvalidSshKeyException;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class PublicKeyAuthenticationPrompt implements SshAuthenticationPrompt {
    private Component parent;
    private PublicKeyAuthenticationClient instance;

    /**
* Creates a new PublicKeyAuthenticationPrompt object.
*
* @param parent
*/
    public PublicKeyAuthenticationPrompt(Component parent) {
        this.parent = parent;
    }

    /**
*
*
* @param instance
*
* @throws AuthenticationProtocolException
*/
    public void setInstance(SshAuthenticationClient instance)
        throws AuthenticationProtocolException {
        if (instance instanceof PublicKeyAuthenticationClient) {
            this.instance = (PublicKeyAuthenticationClient) instance;
        } else {
            throw new AuthenticationProtocolException(
                "PublicKeyAuthenticationClient instance required");
        }
    }

    /**
*
*
* @return
*/
    public boolean showPrompt(SshAuthenticationClient inst)
        throws AuthenticationProtocolException {
        if (inst instanceof PublicKeyAuthenticationClient) {
            instance = (PublicKeyAuthenticationClient) inst;
        } else {
            throw new AuthenticationProtocolException(
                "PublicKeyAuthenticationClient instance required");
        }

        File keyfile = (instance.getKeyfile() == null) ? null
                                                       : new File(instance.getKeyfile());
        String passphrase = null;
        SshPrivateKeyFile pkf = null;
        SshPrivateKey key;

        if ((keyfile == null) || !keyfile.exists()) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileHidingEnabled(false);
            chooser.setDialogTitle("Select Private Key File For Authentication");

            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                keyfile = chooser.getSelectedFile();
            } else {
                return false;
            }
        }

        FileInputStream in = null;

        try {
            pkf = SshPrivateKeyFile.parse(keyfile);
        } catch (InvalidSshKeyException iske) {
            JOptionPane.showMessageDialog(parent, iske.getMessage());

            return false;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(parent, ioe.getMessage());
        }

        // Now see if its passphrase protected
        if (pkf.isPassphraseProtected()) {
            // Show the passphrase dialog
            Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class,
                    parent);
            PassphraseDialog dialog = null;

            if (w instanceof Frame) {
                dialog = new PassphraseDialog((Frame) w);
            } else if (w instanceof Dialog) {
                dialog = new PassphraseDialog((Dialog) w);
            } else {
                dialog = new PassphraseDialog();
            }

            do {
                dialog.setVisible(true);

                if (dialog.isCancelled()) {
                    return false;
                }

                passphrase = new String(dialog.getPassphrase());

                try {
                    key = pkf.toPrivateKey(passphrase);

                    break;
                } catch (InvalidSshKeyException ihke) {
                    dialog.setMessage("Passphrase Invalid! Try again");
                    dialog.setMessageForeground(Color.red);
                }
            } while (true);
        } else {
            try {
                key = pkf.toPrivateKey(passphrase);
            } catch (InvalidSshKeyException ihke) {
                return false;
            }
        }

        instance.setKey(key);
        instance.setKeyfile(keyfile.getAbsolutePath());

        return true;
    }
}
