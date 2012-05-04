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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class ColorComboBox extends JComboBox {
    /**
* Creates a new ColorComboBox object.
*/
    public ColorComboBox() {
        this(null);
    }

    /**
* Creates a new ColorComboBox object.
*
* @param color
*/
    public ColorComboBox(Color color) {
        super(new ColorComboModel());
        setColor(color);
        setRenderer(new ColorRenderer());
        addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (getSelectedItem() == null) {
                        chooseCustomColor();
                    } else {
                        fireChangeEvent();
                    }
                }
            });
    }

    /**
*
*/
    protected void fireChangeEvent() {
        ChangeEvent evt = new ChangeEvent(this);
        ChangeListener[] l = (ChangeListener[]) listenerList.getListeners(ChangeListener.class);

        for (int i = (l.length - 1); i >= 0; i--) {
            l[i].stateChanged(evt);
        }
    }

    /**
*
*
* @param l
*/
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
*
*
* @param l
*/
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    private void chooseCustomColor() {
        Color c = JColorChooser.showDialog(this, "Custom Color", Color.black);

        if (c != null) {
            setColor(c);
            fireChangeEvent();
        }
    }

    /**
*
*
* @param c
*/
    public void setColor(Color c) {
        for (int i = 0; i < (getModel().getSize() - 1); i++) {
            Color z = (Color) getModel().getElementAt(i);

            if (z.equals(c)) {
                setSelectedIndex(i);

                return;
            }
        }

        if (c != null) {
            ((ColorComboModel) getModel()).addColor(c);
        }
    }

    /**
*
*
* @return
*/
    public Color getColor() {
        return (Color) getSelectedItem();
    }

    //  Supporting classes
    static class ColorComboModel extends AbstractListModel
        implements ComboBoxModel {
        private Vector colors = new Vector();
        private Object selected;

        ColorComboModel() {
            colors = new Vector();

            //  Add the initial colors
            colors.addElement(Color.black);
            colors.addElement(Color.white);
            colors.addElement(Color.red.darker());
            colors.addElement(Color.red);
            colors.addElement(Color.orange.darker());
            colors.addElement(Color.orange);
            colors.addElement(Color.yellow.darker());
            colors.addElement(Color.yellow);
            colors.addElement(Color.green.darker());
            colors.addElement(Color.green);
            colors.addElement(Color.blue.darker());
            colors.addElement(Color.blue);
            colors.addElement(Color.cyan.darker());
            colors.addElement(Color.cyan);
            colors.addElement(Color.magenta.darker());
            colors.addElement(Color.magenta);
            colors.addElement(Color.pink.darker());
            colors.addElement(Color.pink);
            colors.addElement(Color.lightGray);
            colors.addElement(Color.gray);
            colors.addElement(Color.darkGray);

            //  Black is initialy selected
            selected = colors.elementAt(0);
        }

        public int getSize() {
            return colors.size() + 1;
        }

        public Object getElementAt(int i) {
            if (i == colors.size()) {
                return null;
            } else {
                return colors.elementAt(i);
            }
        }

        public void setSelectedItem(Object sel) {
            selected = sel;
        }

        public Object getSelectedItem() {
            return selected;
        }

        public void addColor(Color c) {
            int idx = colors.size();
            colors.addElement(c);
            selected = c;
            fireIntervalAdded(this, idx, idx);
        }
    }

    class ColorRenderer extends DefaultListCellRenderer {
        private ColorIcon icon;

        ColorRenderer() {
            icon = new ColorIcon(Color.black, new Dimension(10, 10), Color.black);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        }

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);

            Color c = (Color) value;

            //  If the value is null. Then this signifies custom color
            if (c == null) {
                setIcon(javax.swing.plaf.basic.BasicIconFactory.getCheckBoxIcon());
                setText("Custom ....");
            } else {
                //  Set up the icon
                icon.setColor(c);
                setIcon(icon);

                //  Set the text. If the color is a well known one with a name, render
                //  the name. Otherwise use the RGB values
                String s = "#" + c.getRed() + "," + c.getGreen() + "," +
                    c.getBlue();

                if (c.equals(Color.black)) {
                    s = "Black";
                } else if (c.equals(Color.white)) {
                    s = "White";
                } else if (c.equals(Color.red)) {
                    s = "Red";
                } else if (c.equals(Color.orange)) {
                    s = "Orange";
                } else if (c.equals(Color.yellow)) {
                    s = "Yellow";
                } else if (c.equals(Color.green)) {
                    s = "Green";
                } else if (c.equals(Color.blue)) {
                    s = "Blue";
                } else if (c.equals(Color.cyan)) {
                    s = "Cyan";
                } else if (c.equals(Color.magenta)) {
                    s = "Magenta";
                } else if (c.equals(Color.pink)) {
                    s = "Pink";
                } else if (c.equals(Color.lightGray)) {
                    s = "Light Gray";
                } else if (c.equals(Color.gray)) {
                    s = "Gray";
                } else if (c.equals(Color.darkGray)) {
                    s = "Dark Gray";
                }

                setText(s);
            }

            //
            return this;
        }
    }
}
