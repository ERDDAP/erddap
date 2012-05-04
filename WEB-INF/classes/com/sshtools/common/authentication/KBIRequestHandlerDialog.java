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

import com.sshtools.j2ssh.authentication.KBIPrompt;
import com.sshtools.j2ssh.authentication.KBIRequestHandler;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class KBIRequestHandlerDialog extends JDialog
    implements KBIRequestHandler {
    /**  */
    public final static String KBI_ICON = "largekbi.png";
    boolean cancelled;
    JLabel instructionLabel = new JLabel();
    JPanel buttonsPanel = new JPanel();
    JTextComponent[] promptReply;

    /**
* Creates a new KBIRequestHandlerDialog object.
*/
    public KBIRequestHandlerDialog() {
        super((Frame) null, "", true);
        init();
    }

    /**
* Creates a new KBIRequestHandlerDialog object.
*
* @param frame
*/
    public KBIRequestHandlerDialog(Frame frame) {
        super(frame, "", true);
        init();
    }

    /**
* Creates a new KBIRequestHandlerDialog object.
*
* @param dialog
*/
    public KBIRequestHandlerDialog(Dialog dialog) {
        super(dialog, "", true);
        init();
    }

    void init() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

        JButton ok = new JButton("Ok");
        ok.setMnemonic('o');
        ok.setDefaultCapable(true);
        ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    hide();
                }
            });
        getRootPane().setDefaultButton(ok);

        JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('c');
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    cancelled = true;
                    hide();
                }
            });
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        buttonsPanel.add(cancel);
        buttonsPanel.add(ok);
    }

    /**
*
*
* @param name
* @param instruction
* @param prompts
*/
    public void showPrompts(String name, String instruction, KBIPrompt[] prompts) {
        setTitle(name);
        getContentPane().invalidate();
        getContentPane().removeAll();
        instructionLabel.setText(instruction);

        JPanel promptPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 4, 4);
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.WEST;
        promptReply = new JTextComponent[prompts.length];

        for (int i = 0; i < prompts.length; i++) {
            if (prompts[i].echo()) {
                promptReply[i] = new XTextField(prompts[i].getResponse(), 15);
            } else {
                promptReply[i] = new JPasswordField(prompts[i].getResponse(), 15);
            }

            System.out.println("Creating prompt " + prompts[i].getPrompt() +
                " and setting to " + prompts[i].getResponse());
            gbc.weightx = 0.0;
            UIUtil.jGridBagAdd(promptPanel,
                new JLabel(prompts[i].getPrompt() + " "), gbc,
                GridBagConstraints.RELATIVE);
            gbc.weightx = 1.0;
            UIUtil.jGridBagAdd(promptPanel, promptReply[i], gbc,
                GridBagConstraints.REMAINDER);
        }

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        centerPanel.add(instructionLabel, BorderLayout.NORTH);
        centerPanel.add(promptPanel, BorderLayout.CENTER);

        //  Create the center banner panel
        IconWrapperPanel iconPanel = new IconWrapperPanel(new ResourceIcon(
                    KBIRequestHandlerDialog.class, KBI_ICON), centerPanel);
        iconPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        //  The main panel contains everything and is surrounded by a border
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(iconPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        //  Build the main panel
        getContentPane().setLayout(new GridLayout(1, 1));
        getContentPane().add(mainPanel);
        getContentPane().validate();
        pack();
        UIUtil.positionComponent(SwingConstants.CENTER, this);
        setVisible(true);

        if (!cancelled) {
            for (int i = 0; i < promptReply.length; i++) {
                System.out.println("Setting reply " + i + " to " +
                    promptReply[i].getText());
                prompts[i].setResponse(promptReply[i].getText());
            }
        }
    }
}
