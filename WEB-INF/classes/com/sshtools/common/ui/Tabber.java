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

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class Tabber extends JTabbedPane {
    /**
* Creates a new Tabber object.
*/
    public Tabber() {
        this(TOP);
    }

    /**
* Creates a new Tabber object.
*
* @param tabPlacement
*/
    public Tabber(int tabPlacement) {
        super(tabPlacement);
        addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (getSelectedIndex() != -1) {
                        getTabAt(getSelectedIndex()).tabSelected();
                    }
                }
            });
    }

    /**
*
*
* @param i
*
* @return
*/
    public Tab getTabAt(int i) {
        return ((TabPanel) getComponentAt(i)).getTab();
    }

    /**
*
*
* @return
*/
    public boolean validateTabs() {
        for (int i = 0; i < getTabCount(); i++) {
            Tab tab = ((TabPanel) getComponentAt(i)).getTab();

            if (!tab.validateTab()) {
                setSelectedIndex(i);

                return false;
            }
        }

        return true;
    }

    /**
*
*/
    public void applyTabs() {
        for (int i = 0; i < getTabCount(); i++) {
            Tab tab = ((TabPanel) getComponentAt(i)).getTab();
            tab.applyTab();
        }
    }

    /**
*
*
* @param tab
*/
    public void addTab(Tab tab) {
        addTab(tab.getTabTitle(), tab.getTabIcon(), new TabPanel(tab),
            tab.getTabToolTipText());
    }

    class TabPanel extends JPanel {
        private Tab tab;

        TabPanel(Tab tab) {
            super(new BorderLayout());
            this.tab = tab;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(tab.getTabComponent(), BorderLayout.CENTER);
        }

        public Tab getTab() {
            return tab;
        }
    }
}
