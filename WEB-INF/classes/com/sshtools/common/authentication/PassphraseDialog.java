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

import java.awt.BorderLayout;
import java.awt.Color;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class PassphraseDialog extends JDialog {
    //  Statics
    final static String PASSPHRASE_ICON = "/com/sshtools/common/authentication/largepassphrase.png";
    JButton jButtonCancel = new JButton();
    JButton jButtonOK = new JButton();
    JLabel message = new JLabel("Enter passphrase");
    JPasswordField jPasswordField = new JPasswordField(20);
    boolean userCancelled = false;

    /**
* Creates a new PassphraseDialog object.
*/
    public PassphraseDialog() {
        super((Frame) null, "Passphrase", true);
        init(null);
    }

    /**
* Creates a new PassphraseDialog object.
*
* @param parent
*/
    public PassphraseDialog(Frame parent) {
        super(parent, "Passphrase", true);
        init(parent);
    }

    /**
* Creates a new PassphraseDialog object.
*
* @param parent
* @param identity
*/
    public PassphraseDialog(Frame parent, String identity) {
        super(parent, "Passphrase", true);
        init(parent);
        setTitle(identity + " - Identity");
    }

    /**
* Creates a new PassphraseDialog object.
*
* @param parent
*/
    public PassphraseDialog(Dialog parent) {
        super(parent, "Passphrase", true);
        init(parent);
    }

    /*public void setVisible(boolean visible) {
if (visible) {
UIUtil.positionComponent(UIUtil.CENTER, PassphraseDialog.this);
}
 }*/

    /**
*
*
* @return
*/
    public boolean isCancelled() {
        return userCancelled;
    }

    /**
*
*
* @param message
*/
    public void setMessage(String message) {
        this.message.setText(message);
    }

    /**
*
*
* @param color
*/
    public void setMessageForeground(Color color) {
        message.setForeground(color);
    }

    /**
*
*
* @return
*/
    public char[] getPassphrase() {
        return jPasswordField.getPassword();
    }

    void init(Window parent) {
        getContentPane().setLayout(new GridLayout(1, 1));

        if (parent != null) {
            this.setLocationRelativeTo(parent);
        }

        try {
            jbInit();
            pack();
            UIUtil.positionComponent(UIUtil.CENTER, PassphraseDialog.this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jButtonCancel_actionPerformed(ActionEvent e) {
        userCancelled = true;
        hide();
    }

    void jButtonOK_actionPerformed(ActionEvent e) {
        userCancelled = false;
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

        //  Passphrase panel
        JPanel passphrasePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 2, 2, 2);
        gbc.weightx = 1.0;
        UIUtil.jGridBagAdd(passphrasePanel, message, gbc,
            GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(passphrasePanel, jPasswordField, gbc,
            GridBagConstraints.REMAINDER);

        //  Create the center banner panel
        IconWrapperPanel centerPanel = new IconWrapperPanel(new ResourceIcon(
                    PASSPHRASE_ICON), passphrasePanel);
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
