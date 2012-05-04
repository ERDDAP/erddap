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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class OptionsDialog extends JDialog implements ActionListener {
    private Option selectedOption;
    private OptionCallback callback;
    private JButton defaultButton;

    /**
* Creates a new OptionsDialog object.
*
* @param parent
* @param options
* @param message
* @param title
* @param defaultOption
* @param callback
* @param modal
* @param icon
*/
    public OptionsDialog(JDialog parent, Option[] options, Object message,
        String title, Option defaultOption, OptionCallback callback,
        boolean modal, Icon icon) {
        super(parent, title, modal);
        init(options, message, defaultOption, callback, icon);
    }

    /**
* Creates a new OptionsDialog object.
*
* @param parent
* @param options
* @param message
* @param title
* @param defaultOption
* @param callback
* @param modal
* @param icon
*/
    public OptionsDialog(JFrame parent, Option[] options, Object message,
        String title, Option defaultOption, OptionCallback callback,
        boolean modal, Icon icon) {
        super(parent, title, modal);
        init(options, message, defaultOption, callback, icon);
    }

    private void init(Option[] options, Object message, Option defaultOption,
        OptionCallback callback, Icon icon) {
        //
        this.callback = callback;

        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        b.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        for (int i = 0; i < options.length; i++) {
            JButton button = new JButton(options[i].getText());

            if (options[i] == defaultOption) {
                button.setDefaultCapable(options[i] == defaultOption);
                defaultButton = button;
            }

            button.setMnemonic(options[i].getMnemonic());
            button.setToolTipText(options[i].getToolTipText());
            button.putClientProperty("option", options[i]);
            button.addActionListener(this);
            b.add(button);
        }

        //
        JPanel s = new JPanel(new BorderLayout());
        s.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        s.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.NORTH);
        s.add(b, BorderLayout.SOUTH);

        //
        JPanel z = new JPanel(new BorderLayout());
        z.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        //
        if (message instanceof JComponent) {
            z.add((JComponent) message, BorderLayout.CENTER);
        } else {
            z.add(new MultilineLabel(String.valueOf(message)),
                BorderLayout.CENTER);
        }

        //  Icon panel
        JLabel i = null;

        if (icon != null) {
            i = new JLabel(icon);
            i.setVerticalAlignment(JLabel.NORTH);
            i.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        }

        //  Build this panel
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(z, BorderLayout.CENTER);

        if (i != null) {
            getContentPane().add(i, BorderLayout.WEST);
        }

        getContentPane().add(s, BorderLayout.SOUTH);

        //
        pack();
    }

    /**
*
*
* @return
*/
    public JButton getDefaultButton() {
        return defaultButton;
    }

    /**
*
*
* @return
*/
    public Option getSelectedOption() {
        return selectedOption;
    }

    /**
*
*
* @param evt
*/
    public void actionPerformed(ActionEvent evt) {
        selectedOption = (Option) ((JButton) evt.getSource()).getClientProperty(
                "option");

        if ((callback == null) || callback.canClose(this, selectedOption)) {
            setVisible(false);
        }
    }

    /**
*
*
* @param parent
* @param options
* @param message
* @param title
* @param defaultOption
* @param callback
* @param icon
*
* @return
*/
    public static OptionsDialog createOptionDialog(JComponent parent,
        Option[] options, Object message, String title, Option defaultOption,
        OptionCallback callback, Icon icon) {
        //
        OptionsDialog dialog = null;
        Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class,
                parent);

        if (w instanceof JFrame) {
            dialog = new OptionsDialog((JFrame) w, options, message, title,
                    defaultOption, callback, true, icon);
        } else if (w instanceof JDialog) {
            dialog = new OptionsDialog((JDialog) w, options, message, title,
                    defaultOption, callback, true, icon);
        } else {
            dialog = new OptionsDialog((JFrame) null, options, message, title,
                    defaultOption, callback, true, icon);
        }

        if (dialog.getDefaultButton() != null) {
            dialog.getRootPane().setDefaultButton(dialog.getDefaultButton());
        }

        return dialog;
    }
}
