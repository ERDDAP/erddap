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

import com.sshtools.common.ui.IconWrapperPanel;
import com.sshtools.common.ui.ResourceIcon;
import com.sshtools.common.ui.UIUtil;
import com.sshtools.common.ui.XTextField;

import com.sshtools.j2ssh.authentication.AuthenticationProtocolException;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationPrompt;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.16 $
 */
public class PasswordAuthenticationDialog extends JDialog
    implements SshAuthenticationPrompt {
    //  Statics
    final static String KEY_ICON = "largecard.png";
    PasswordAuthenticationClient instance;
    JButton jButtonCancel = new JButton();
    JButton jButtonOK = new JButton();
    JPasswordField jPasswordField = new JPasswordField(20);
    XTextField jTextUsername = new XTextField(20);
    boolean userCancelled = false;

    /**
* Creates a new PasswordAuthenticationDialog object.
*/
    public PasswordAuthenticationDialog() {
        super((Frame) null, "Password Authentication", true);
        init(null);
    }

    /**
* Creates a new PasswordAuthenticationDialog object.
*
* @param parent
*/
    public PasswordAuthenticationDialog(Frame parent) {
        super(parent, "Password Authentication", true);
        init(parent);
    }

    /**
* Creates a new PasswordAuthenticationDialog object.
*
* @param parent
*/
    public PasswordAuthenticationDialog(Dialog parent) {
        super(parent, "Password Authentication", true);
        init(parent);
    }

    /**
*
*
* @return
*/
    public String getPassword() {
        return String.valueOf(jPasswordField.getPassword());
    }

    /**
*
*
* @return
*/
    public String getUsername() {
        return jTextUsername.getText();
    }

    /**
*
*
* @param username
*/
    public void setUsername(String username) {
        if (username != null) {
            jTextUsername.setText(username);
        }
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
        if (instance instanceof PasswordAuthenticationClient) {
            this.instance = (PasswordAuthenticationClient) instance;
        } else {
            throw new AuthenticationProtocolException(
                "PasswordAuthenticationClient instance required");
        }
    }

    /**
*
*
* @return
*/
    public boolean showPrompt(SshAuthenticationClient inst)
        throws AuthenticationProtocolException {
        if (inst instanceof PasswordAuthenticationClient) {
            instance = (PasswordAuthenticationClient) inst;

            if (instance.getUsername() != null) {
                jTextUsername.setText(instance.getUsername());
            }

            if (!jTextUsername.getText().equals("")) {
                jPasswordField.grabFocus();
            }

            UIUtil.positionComponent(SwingConstants.CENTER, this);
            setVisible(true);

            if (!userCancelled) {
                instance.setUsername(getUsername());
                instance.setPassword(getPassword());

                return true;
            } else {
                return false;
            }
        } else {
            throw new AuthenticationProtocolException(
                "PasswordAuthenticationClient instance required");
        }
    }

    void init(Window parent) {
        setModal(true);

        getContentPane().setLayout(new GridLayout(1, 1));

        if (parent != null) {
            try {
                Method m = getClass().getMethod("setLocationRelativeTo",
                        new Class[] { parent.getClass() });
                m.invoke(this, new Object[] { parent });
            } catch (Throwable t) {
                //
            }
        }

        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jButtonCancel_actionPerformed(ActionEvent e) {
        userCancelled = true;
        hide();
    }

    void jButtonOK_actionPerformed(ActionEvent e) {
        if (jTextUsername.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, "You must enter a username!",
                "Password Authentication", JOptionPane.OK_OPTION);

            return;
        }

        hide();
    }

    void jbInit() throws Exception {
        // Add a window listener to see when the window closes without
        // selecting OK
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    userCancelled = true;
                }
            });

        //  Ok button
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    jButtonOK_actionPerformed(e);
                }
            });
        jButtonOK.setText("OK");
        jButtonOK.setMnemonic('o');
        getRootPane().setDefaultButton(jButtonOK);

        //  Cancel button
        jButtonCancel.setText("Cancel");
        jButtonCancel.setMnemonic('c');
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    jButtonCancel_actionPerformed(e);
                }
            });

        //  User / password panel
        JPanel userPasswordPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 2, 2, 2);
        gbc.weightx = 1.0;

        //  Username
        UIUtil.jGridBagAdd(userPasswordPanel, new JLabel("User"), gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(userPasswordPanel, jTextUsername, gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

        //  Username
        UIUtil.jGridBagAdd(userPasswordPanel, new JLabel("Password"), gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(userPasswordPanel, jPasswordField, gbc,
            GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

        //  Create the center banner panel
        IconWrapperPanel centerPanel = new IconWrapperPanel(new ResourceIcon(
                    PasswordAuthenticationDialog.class, KEY_ICON),
                userPasswordPanel);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        //
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(6, 6, 0, 0);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(buttonPanel, jButtonOK, gbc,
            GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(buttonPanel, jButtonCancel, gbc,
            GridBagConstraints.REMAINDER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.add(buttonPanel);

        //  Wrap the whole thing in an empty border
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        //  Build the main panel
        getContentPane().add(mainPanel);

        //
        jPasswordField.grabFocus();
    }
}
