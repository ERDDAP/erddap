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
import com.sshtools.common.ui.SshToolsConnectionHostTab;
import com.sshtools.common.ui.UIUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class AuthenticationDialog extends JDialog {
    JList jListAuths = new JList();
    JLabel messageLabel = new JLabel();
    boolean cancelled = false;

    /**
* Creates a new AuthenticationDialog object.
*/
    public AuthenticationDialog() {
        super((Frame) null, "Select Authentication Method(s)", true);
        init();
    }

    /**
* Creates a new AuthenticationDialog object.
*
* @param frame
*/
    public AuthenticationDialog(Frame frame) {
        super(frame, "Select Authentication Method(s)", true);
        init();
    }

    /**
* Creates a new AuthenticationDialog object.
*
* @param dialog
*/
    public AuthenticationDialog(Dialog dialog) {
        super(dialog, "Select Authentication Method(s)", true);
        init();
    }

    void init() {
        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setMethodList(java.util.List methods) {
        jListAuths.setListData(methods.toArray());

        if (methods.size() > 0) {
            jListAuths.setSelectedIndex(0);
        }
    }

    void jbInit() throws Exception {
        //
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        //
        messageLabel.setForeground(Color.red);
        messageLabel.setHorizontalAlignment(JLabel.CENTER);

        //  Create the list of available methods and put in in a scroll panel
        jListAuths = new JList();
        jListAuths.setVisibleRowCount(5);

        JPanel listPanel = new JPanel(new GridLayout(1, 1));
        listPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        listPanel.add(new JScrollPane(jListAuths));

        //  Main panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        centerPanel.add(new JLabel(
                "Please select an authentication method(s) to continue."),
            BorderLayout.NORTH);
        centerPanel.add(listPanel, BorderLayout.CENTER);

        //  Create the bottom button panel
        JButton proceed = new JButton("Proceed");
        proceed.setMnemonic('p');
        proceed.setDefaultCapable(true);
        proceed.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    //  I presume this component **is** reused?
                    hide();
                }
            });
        getRootPane().setDefaultButton(proceed);

        JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('c');
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    cancelled = true;
                    hide();
                }
            });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        southPanel.add(cancel);
        southPanel.add(proceed);

        //  Create the center banner panel
        IconWrapperPanel iconPanel = new IconWrapperPanel(new ResourceIcon(
                    SshToolsConnectionHostTab.AUTH_ICON), centerPanel);
        iconPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        //  The main panel contains everything and is surrounded by a border
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(messageLabel, BorderLayout.NORTH);
        mainPanel.add(iconPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        //  Build the main panel
        getContentPane().setLayout(new GridLayout(1, 1));
        getContentPane().add(mainPanel);
    }

    /**
*
*
* @param parent
* @param support
*
* @return
*/
    public static java.util.List showAuthenticationDialog(Component parent,
        java.util.List support) {
        return showAuthenticationDialog(parent, support, null);
    }

    /**
*
*
* @param parent
* @param support
* @param message
*
* @return
*/
    public static java.util.List showAuthenticationDialog(Component parent,
        java.util.List support, String message) {
        Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class,
                parent);
        AuthenticationDialog dialog = null;

        if (w instanceof Frame) {
            dialog = new AuthenticationDialog((Frame) w);
        } else if (w instanceof Dialog) {
            dialog = new AuthenticationDialog((Dialog) w);
        } else {
            dialog = new AuthenticationDialog();
        }

        UIUtil.positionComponent(SwingConstants.CENTER, dialog);

        return dialog.showAuthenticationMethods(support, message);
    }

    /**
*
*
* @param supported
* @param message
*
* @return
*/
    public java.util.List showAuthenticationMethods(java.util.List supported,
        String message) {
        // Set the list
        this.setMethodList(supported);

        // Show the dialog
        UIUtil.positionComponent(SwingConstants.CENTER, this);

        if (message != null) {
            messageLabel.setVisible(true);
            messageLabel.setText(message);
        } else {
            messageLabel.setVisible(false);
        }

        pack();
        toFront();
        setVisible(true);

        // Put the selected values into a new list and return
        java.util.List list = new ArrayList();

        if (!cancelled) {
            Object[] methods = jListAuths.getSelectedValues();

            if (methods != null) {
                for (int i = 0; i < methods.length; i++) {
                    list.add(methods[i]);
                }
            }
        }

        return list;
    }

    void jButtonProceed_actionPerformed(ActionEvent e) {
        hide();
    }
}
