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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class StatusBar extends JPanel {
    /**  */
    public final static Icon GREEN_LED_ON = new ResourceIcon(StatusBar.class,
            "greenledon.png");

    /**  */
    public final static Icon GREEN_LED_OFF = new ResourceIcon(StatusBar.class,
            "greenledoff.png");

    /**  */
    public final static Icon RED_LED_ON = new ResourceIcon(StatusBar.class,
            "redledon.png");

    /**  */
    public final static Icon RED_LED_OFF = new ResourceIcon(StatusBar.class,
            "redledoff.png");

    //
    private StatusLabel connected;

    //
    private StatusLabel statusText;

    //
    private StatusLabel host;

    //
    private StatusLabel user;

    //
    private StatusLabel rid;
    private JLabel sending;
    private JLabel receiving;
    private javax.swing.Timer sendingTimer;
    private javax.swing.Timer receivingTimer;

    /**
* Creates a new StatusBar object.
*/
    public StatusBar() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.0;
        connected = new StatusLabel(RED_LED_OFF);
        connected.setHorizontalAlignment(JLabel.CENTER);
        UIUtil.jGridBagAdd(this, connected, gbc, 1);

        JPanel lights = new JPanel(new GridLayout(1, 2));
        sending = new JLabel(GREEN_LED_OFF);
        sending.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        sending.setHorizontalAlignment(JLabel.CENTER);
        receiving = new JLabel(GREEN_LED_OFF);
        receiving.setHorizontalAlignment(JLabel.CENTER);
        lights.add(sending);
        lights.add(receiving);
        gbc.weightx = 0.0;
        lights.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        gbc.weightx = 1.5;
        host = new StatusLabel();
        UIUtil.jGridBagAdd(this, host, gbc, 1);
        user = new StatusLabel();
        UIUtil.jGridBagAdd(this, user, gbc, 1);
        rid = new StatusLabel();
        UIUtil.jGridBagAdd(this, rid, gbc, 1);
        statusText = new StatusLabel();
        gbc.weightx = 4.0;
        UIUtil.jGridBagAdd(this, statusText, gbc, GridBagConstraints.RELATIVE);
        gbc.weightx = 0.0;
        UIUtil.jGridBagAdd(this, lights, gbc, GridBagConstraints.REMAINDER);
    }

    /**
*
*
* @param receiving
*/
    public void setReceiving(boolean receiving) {
        this.receiving.setIcon(receiving ? GREEN_LED_ON : GREEN_LED_OFF);
    }

    /**
*
*
* @param sending
*/
    public void setSending(boolean sending) {
        this.sending.setIcon(sending ? GREEN_LED_ON : GREEN_LED_OFF);
    }

    /**
*
*
* @param connected
*/
    public void setConnected(boolean connected) {
        this.connected.setIcon(connected ? RED_LED_ON : RED_LED_OFF);
    }

    /**
*
*
* @param text
*/
    public void setStatusText(String text) {
        statusText.setText(text);
    }

    /**
*
*
* @param text
*/
    public void setHost(String text) {
        host.setText(text);
    }

    /**
*
*
* @param text
* @param port
*/
    public void setHost(String text, int port) {
        host.setText(text + ":" + String.valueOf(port));
    }

    /**
*
*
* @param remoteId
*/
    public void setRemoteId(String remoteId) {
        rid.setText(remoteId);
    }

    /**
*
*
* @param text
*/
    public void setUser(String text) {
        user.setText(text);
    }

    class StatusLabel extends JLabel {
        StatusLabel(Icon icon) {
            super(icon);
            init();
        }

        StatusLabel() {
            super();
            init();
        }

        void init() {
            setFont(getFont().deriveFont(10f));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLoweredBevelBorder(),
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        }
    }
}
