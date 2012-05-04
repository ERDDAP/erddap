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
package com.sshtools.common.keygen;

import com.sshtools.common.ui.NumericTextField;
import com.sshtools.common.ui.UIUtil;
import com.sshtools.common.ui.XTextField;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class KeygenPanel extends JPanel implements DocumentListener,
    ActionListener {
    /**  */
    public final static int GENERATE_KEY_PAIR = 0;

    /**  */
    public final static int CONVERT_IETF_SECSH_TO_OPENSSH = 1;

    /**  */
    public final static int CONVERT_OPENSSH_TO_IETF_SECSH = 2;

    /**  */
    public final static int CHANGE_PASSPHRASE = 3;

    //  Private instance variables
    private JButton browseInput;

    //  Private instance variables
    private JButton browseOutput;
    private JComboBox action;
    private JComboBox type;
    private JLabel bitsLabel;
    private JLabel inputFileLabel;
    private JLabel newPassphraseLabel;
    private JLabel oldPassphraseLabel;
    private JLabel outputFileLabel;
    private JLabel typeLabel;
    private JPasswordField newPassphrase;
    private JPasswordField oldPassphrase;
    private JProgressBar strength;
    private XTextField inputFile;
    private XTextField outputFile;
    private NumericTextField bits;

    /**
* Creates a new KeygenPanel object.
*/
    public KeygenPanel() {
        super();

        JPanel keyPanel = new JPanel(new GridBagLayout());
        keyPanel.setBorder(BorderFactory.createTitledBorder("Key"));

        //  Create the main panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        Insets normalInsets = new Insets(0, 2, 4, 2);
        Insets indentedInsets = new Insets(0, 26, 4, 2);
        gbc.insets = normalInsets;
        gbc.weightx = 0.0;

        //  Action
        UIUtil.jGridBagAdd(keyPanel, new JLabel("Action"), gbc, 1);
        gbc.weightx = 1.0;
        action = new JComboBox(new String[] {
                    "Generate key pair", "Convert IETF SECSH to OpenSSH",
                    "Convert OpenSSH to IETF SECSH", "Change passphrase"
                });
        action.addActionListener(this);
        gbc.weightx = 2.0;
        UIUtil.jGridBagAdd(keyPanel, action, gbc, GridBagConstraints.RELATIVE);
        gbc.weightx = 0.0;
        UIUtil.jGridBagAdd(keyPanel, new JLabel(), gbc,
            GridBagConstraints.REMAINDER);
        gbc.insets = indentedInsets;

        //  File
        inputFileLabel = new JLabel("Input File");
        UIUtil.jGridBagAdd(keyPanel, inputFileLabel, gbc, 1);
        gbc.insets = normalInsets;
        gbc.weightx = 1.0;
        inputFile = new XTextField(20);
        UIUtil.jGridBagAdd(keyPanel, inputFile, gbc, GridBagConstraints.RELATIVE);
        inputFileLabel.setLabelFor(inputFile);
        gbc.weightx = 0.0;
        browseInput = new JButton("Browse");
        browseInput.setMnemonic('b');
        browseInput.addActionListener(this);
        UIUtil.jGridBagAdd(keyPanel, browseInput, gbc,
            GridBagConstraints.REMAINDER);

        //  File
        gbc.insets = indentedInsets;
        outputFileLabel = new JLabel("Output File");
        UIUtil.jGridBagAdd(keyPanel, outputFileLabel, gbc, 1);
        gbc.insets = normalInsets;
        gbc.weightx = 1.0;
        outputFile = new XTextField(20);
        UIUtil.jGridBagAdd(keyPanel, outputFile, gbc,
            GridBagConstraints.RELATIVE);
        gbc.weightx = 0.0;
        outputFileLabel.setLabelFor(outputFile);
        browseOutput = new JButton("Browse");
        browseOutput.setMnemonic('r');
        browseOutput.addActionListener(this);
        UIUtil.jGridBagAdd(keyPanel, browseOutput, gbc,
            GridBagConstraints.REMAINDER);

        //  Old Passphrase
        gbc.insets = indentedInsets;
        oldPassphraseLabel = new JLabel("Old Passphrase");
        UIUtil.jGridBagAdd(keyPanel, oldPassphraseLabel, gbc, 1);
        gbc.insets = normalInsets;
        gbc.weightx = 2.0;
        oldPassphrase = new JPasswordField(20);
        oldPassphrase.setBackground(Color.white);
        oldPassphrase.getDocument().addDocumentListener(this);
        oldPassphraseLabel.setLabelFor(oldPassphrase);
        UIUtil.jGridBagAdd(keyPanel, oldPassphrase, gbc,
            GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(keyPanel, new JLabel(), gbc,
            GridBagConstraints.REMAINDER);

        //  Passphrase
        gbc.insets = indentedInsets;
        newPassphraseLabel = new JLabel("New Passphrase");
        UIUtil.jGridBagAdd(keyPanel, newPassphraseLabel, gbc, 1);
        gbc.insets = normalInsets;
        gbc.weightx = 2.0;
        newPassphrase = new JPasswordField(20);
        newPassphrase.setBackground(Color.white);
        newPassphrase.getDocument().addDocumentListener(this);
        newPassphraseLabel.setLabelFor(newPassphrase);
        UIUtil.jGridBagAdd(keyPanel, newPassphrase, gbc,
            GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(keyPanel, new JLabel(), gbc,
            GridBagConstraints.REMAINDER);

        //  Bits
        gbc.insets = indentedInsets;
        bitsLabel = new JLabel("Bits");
        UIUtil.jGridBagAdd(keyPanel, bitsLabel, gbc, 1);
        gbc.weightx = 2.0;
        gbc.insets = normalInsets;
        bits = new NumericTextField(new Integer(512), new Integer(1024),
                new Integer(1024));
        bitsLabel.setLabelFor(bits);
        UIUtil.jGridBagAdd(keyPanel, bits, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(keyPanel, new JLabel(), gbc,
            GridBagConstraints.REMAINDER);

        //  Type
        gbc.insets = indentedInsets;
        typeLabel = new JLabel("Type");
        UIUtil.jGridBagAdd(keyPanel, typeLabel, gbc, 1);
        gbc.insets = normalInsets;
        gbc.weightx = 2.0;
        type = new JComboBox(new String[] { "DSA", "RSA" });
        type.setFont(inputFile.getFont());

        //  Combo boxes look crap in metal
        UIUtil.jGridBagAdd(keyPanel, type, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(keyPanel, new JLabel(), gbc,
            GridBagConstraints.REMAINDER);
        strength = new JProgressBar(0, 40);
        strength.setStringPainted(true);

        JPanel strengthPanel = new JPanel(new GridLayout(1, 1));
        strengthPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Passphrase strength"),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        strengthPanel.add(strength);

        //  Build this panel
        setLayout(new BorderLayout());
        add(keyPanel, BorderLayout.CENTER);
        add(strengthPanel, BorderLayout.SOUTH);
        calculateStrength();
        setAvailableActions();
    }

    /**
*
*
* @return
*/
    public int getAction() {
        return action.getSelectedIndex();
    }

    /**
*
*
* @return
*/
    public int getBits() {
        return ((Integer) bits.getValue()).intValue();
    }

    /**
*
*
* @return
*/
    public String getInputFilename() {
        return inputFile.getText();
    }

    /**
*
*
* @return
*/
    public char[] getNewPassphrase() {
        return newPassphrase.getPassword();
    }

    /**
*
*
* @return
*/
    public char[] getOldPassphrase() {
        return oldPassphrase.getPassword();
    }

    /**
*
*
* @return
*/
    public String getOutputFilename() {
        return outputFile.getText();
    }

    /**
*
*
* @return
*/
    public String getType() {
        return (String) type.getSelectedItem();
    }

    /**
*
*
* @param evt
*/
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == browseOutput) {
            File f = new File(outputFile.getText());
            JFileChooser chooser = new JFileChooser(f);
            chooser.setSelectedFile(f);
            chooser.setDialogTitle("Choose output file ..");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                outputFile.setText(chooser.getSelectedFile().getPath());
            }
        } else if (evt.getSource() == browseInput) {
            File f = new File(inputFile.getText());
            JFileChooser chooser = new JFileChooser(f);
            chooser.setSelectedFile(f);
            chooser.setDialogTitle("Choose input file ..");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                inputFile.setText(chooser.getSelectedFile().getPath());
            }
        } else {
            setAvailableActions();
        }
    }

    /**
*
*
* @param e
*/
    public void changedUpdate(DocumentEvent e) {
        calculateStrength();
    }

    /**
*
*
* @param e
*/
    public void insertUpdate(DocumentEvent e) {
        calculateStrength();
    }

    /**
*
*
* @param e
*/
    public void removeUpdate(DocumentEvent e) {
        calculateStrength();
    }

    private void setAvailableActions() {
        inputFile.setEnabled((getAction() == CONVERT_IETF_SECSH_TO_OPENSSH) ||
            (getAction() == CONVERT_OPENSSH_TO_IETF_SECSH) ||
            (getAction() == CHANGE_PASSPHRASE));
        inputFileLabel.setEnabled(inputFile.isEnabled());
        browseInput.setEnabled(inputFile.isEnabled());
        bits.setEnabled(getAction() == GENERATE_KEY_PAIR);
        bitsLabel.setEnabled(bits.isEnabled());
        outputFile.setEnabled((getAction() == CONVERT_IETF_SECSH_TO_OPENSSH) ||
            (getAction() == CONVERT_OPENSSH_TO_IETF_SECSH) ||
            (getAction() == GENERATE_KEY_PAIR));
        outputFileLabel.setEnabled(outputFile.isEnabled());
        browseOutput.setEnabled(outputFile.isEnabled());
        newPassphrase.setEnabled((getAction() == GENERATE_KEY_PAIR) ||
            (getAction() == CHANGE_PASSPHRASE));
        newPassphraseLabel.setEnabled(newPassphrase.isEnabled());
        oldPassphrase.setEnabled(getAction() == CHANGE_PASSPHRASE);
        oldPassphraseLabel.setEnabled(oldPassphrase.isEnabled());
        type.setEnabled(getAction() == GENERATE_KEY_PAIR);
        typeLabel.setEnabled(type.isEnabled());

        if (inputFile.isEnabled()) {
            inputFile.requestFocus();
        } else {
            outputFile.requestFocus();
        }
    }

    private void calculateStrength() {
        char[] pw = newPassphrase.getPassword();
        strength.setValue((pw.length < 40) ? pw.length : 40);

        Color f;
        String t;

        if (pw.length == 0) {
            f = Color.red;
            t = "Empty!!";
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append(pw.length);
            buf.append(" characters - ");

            if (pw.length < 10) {
                f = Color.red;
                buf.append("Weak!");
            } else if (pw.length < 20) {
                f = Color.orange;
                buf.append("Ok");
            } else if (pw.length < 30) {
                f = Color.green.darker();
                buf.append("Strong");
            } else {
                f = Color.green;
                buf.append("Very strong!");
            }

            t = buf.toString();
        }

        strength.setString(t);
        strength.setForeground(f);
    }
}
