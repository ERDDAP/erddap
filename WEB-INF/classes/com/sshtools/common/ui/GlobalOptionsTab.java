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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class GlobalOptionsTab extends JPanel implements OptionsTab {
    /**  */
    public final static String GLOBAL_ICON = "largeglobal.png";

    //
    private JComboBox lafChooser;

    /**
* Creates a new GlobalOptionsTab object.
*/
    public GlobalOptionsTab() {
        super();

        Insets ins = new Insets(2, 2, 2, 2);
        JPanel s = new JPanel(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.weighty = 1.0;
        gbc1.insets = ins;
        gbc1.anchor = GridBagConstraints.NORTHWEST;
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        gbc1.weightx = 0.0;
        UIUtil.jGridBagAdd(s, new JLabel("Look and feel"), gbc1,
            GridBagConstraints.RELATIVE);
        gbc1.weightx = 1.0;
        UIUtil.jGridBagAdd(s,
            lafChooser = new JComboBox(
                    SshToolsApplication.getAllLookAndFeelInfo()), gbc1,
            GridBagConstraints.REMAINDER);
        lafChooser.setRenderer(new LAFRenderer());

        IconWrapperPanel w = new IconWrapperPanel(new ResourceIcon(
                    GlobalOptionsTab.class, GLOBAL_ICON), s);

        //  This tab
        setLayout(new BorderLayout());
        add(w, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        reset();
    }

    /**
*
*/
    public void reset() {
        String sel = PreferencesStore.get(SshToolsApplication.PREF_LAF,
                UIManager.getLookAndFeel().getClass().getName());

        for (int i = 0; i < lafChooser.getModel().getSize(); i++) {
            if (((UIManager.LookAndFeelInfo) lafChooser.getModel().getElementAt(i)).getClassName()
                     .equals(sel)) {
                lafChooser.setSelectedIndex(i);

                break;
            }
        }
    }

    /**
*
*
* @return
*/
    public String getTabContext() {
        return "Options";
    }

    /**
*
*
* @return
*/
    public Icon getTabIcon() {
        return null;
    }

    /**
*
*
* @return
*/
    public String getTabTitle() {
        return "Global";
    }

    /**
*
*
* @return
*/
    public String getTabToolTipText() {
        return "Global options.";
    }

    /**
*
*
* @return
*/
    public int getTabMnemonic() {
        return 'g';
    }

    /**
*
*
* @return
*/
    public Component getTabComponent() {
        return this;
    }

    /**
*
*
* @return
*/
    public boolean validateTab() {
        return true;
    }

    /**
*
*/
    public void applyTab() {
        PreferencesStore.put(SshToolsApplication.PREF_LAF,
            ((UIManager.LookAndFeelInfo) lafChooser.getSelectedItem()).getClassName());
    }

    /**
*
*/
    public void tabSelected() {
    }

    class LAFRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);
            setText(((UIManager.LookAndFeelInfo) value).getName());

            return this;
        }
    }
}
