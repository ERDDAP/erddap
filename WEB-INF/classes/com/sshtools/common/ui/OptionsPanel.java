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
package com.sshtools.common.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class OptionsPanel extends JPanel {
    //

    /**  */
    protected Log log = LogFactory.getLog(OptionsPanel.class);

    //
    private Tabber tabber;
    private boolean cancelled;

    /**
* Creates a new OptionsPanel object.
*
* @param optionalTabs
*/
    public OptionsPanel(OptionsTab[] optionalTabs) {
        super();
        tabber = new Tabber();

        if (optionalTabs != null) {
            for (int i = 0; i < optionalTabs.length; i++) {
                optionalTabs[i].reset();
                addTab(optionalTabs[i]);
            }
        }

        //  Add the common tabs
        addTab(new GlobalOptionsTab());

        //  Build this panel
        setLayout(new GridLayout(1, 1));
        add(tabber);
    }

    /**
*
*
* @return
*/
    public boolean validateTabs() {
        return tabber.validateTabs();
    }

    /**
*
*/
    public void applyTabs() {
        tabber.applyTabs();
    }

    /**
*
*
* @param tab
*/
    public void addTab(OptionsTab tab) {
        tabber.addTab(tab);
    }

    /**
*
*/
    public void reset() {
        for (int i = 0; i < tabber.getTabCount(); i++) {
            ((OptionsTab) tabber.getTabAt(i)).reset();
        }
    }

    /**
*
*
* @param parent
* @param optionalTabs
*
* @return
*/
    public static boolean showOptionsDialog(Component parent,
        OptionsTab[] optionalTabs) {
        final OptionsPanel opts = new OptionsPanel(optionalTabs);
        opts.reset();

        JDialog d = null;
        Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class,
                parent);

        if (w instanceof JDialog) {
            d = new JDialog((JDialog) w, "Options", true);
        } else if (w instanceof JFrame) {
            d = new JDialog((JFrame) w, "Options", true);
        } else {
            d = new JDialog((JFrame) null, "Options", true);
        }

        final JDialog dialog = d;

        //  Create the bottom button panel
        final JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('c');
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    opts.cancelled = true;
                    dialog.setVisible(false);
                }
            });

        final JButton ok = new JButton("Ok");
        ok.setMnemonic('o');
        ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (opts.validateTabs()) {
                        dialog.setVisible(false);
                    }
                }
            });
        dialog.getRootPane().setDefaultButton(ok);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(6, 6, 0, 0);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(buttonPanel, ok, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(buttonPanel, cancel, gbc,
            GridBagConstraints.REMAINDER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.add(buttonPanel);

        //
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(opts, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Show the dialog
        dialog.getContentPane().setLayout(new GridLayout(1, 1));
        dialog.getContentPane().add(mainPanel);
        dialog.pack();
        dialog.setResizable(true);
        UIUtil.positionComponent(SwingConstants.CENTER, dialog);
        dialog.setVisible(true);

        if (!opts.cancelled) {
            opts.applyTabs();
        }

        return !opts.cancelled;
    }
}
