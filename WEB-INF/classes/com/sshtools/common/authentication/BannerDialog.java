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
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class BannerDialog extends JDialog {
    //  Statics
    final static String BANNER_ICON = "largebanner.png";

    //  Private instance variables
    private JTextArea text;

    /**
* Creates a new BannerDialog object.
*
* @param bannerText
*/
    public BannerDialog(String bannerText) {
        super((Frame) null, "SSH Authentication - Banner Message", true);
        init(bannerText);
    }

    /**
* Creates a new BannerDialog object.
*
* @param parent
* @param bannerText
*/
    public BannerDialog(Frame parent, String bannerText) {
        super(parent, "SSH Authentication - Banner Message", true);
        init(bannerText);
    }

    /**
* Creates a new BannerDialog object.
*
* @param parent
* @param bannerText
*/
    public BannerDialog(Dialog parent, String bannerText) {
        super(parent, "SSH Authentication - Banner Message", true);
        init(bannerText);
    }

    void init(String bannerText) {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //
        setText(bannerText);
    }

    /**
*
*
* @param parent
* @param bannerText
*/
    public static void showBannerDialog(Component parent, String bannerText) {
        Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class,
                parent);
        BannerDialog dialog = null;

        if (w instanceof Frame) {
            dialog = new BannerDialog((Frame) w, bannerText);
        } else if (w instanceof Dialog) {
            dialog = new BannerDialog((Dialog) w, bannerText);
        } else {
            dialog = new BannerDialog(bannerText);
        }

        UIUtil.positionComponent(SwingConstants.CENTER, dialog);
        dialog.toFront();
        dialog.setVisible(true);
    }

    /**
*
*
* @param text
*/
    public void setText(String text) {
        this.text.setText(text);
        this.repaint();
    }

    void jbInit() throws Exception {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        //  Create the component to display the banner text
        text = new JTextArea();
        text.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        //        text.setLineWrap(true);
        text.setEditable(false);

        Font f = new Font("MonoSpaced", text.getFont().getStyle(),
                text.getFont().getSize());
        text.setFont(f);

        JScrollPane textScroller = new JScrollPane(text);

        //  Create the center banner panel
        IconWrapperPanel centerPanel = new IconWrapperPanel(new ResourceIcon(
                    BannerDialog.class, BANNER_ICON), textScroller);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        //  Create the south button panel
        JButton ok = new JButton("Ok");
        ok.setMnemonic('o');
        ok.setDefaultCapable(true);
        ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    //  I presume this component is not reused?
                    dispose();
                }
            });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(ok);
        getRootPane().setDefaultButton(ok);

        //  Build the main panel
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(centerPanel, BorderLayout.CENTER);
        getContentPane().add(southPanel, BorderLayout.SOUTH);

        //
        setSize(500, 245);
        setResizable(false);
    }
}
