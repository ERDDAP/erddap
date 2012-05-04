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

import com.sshtools.common.util.PropertyUtil;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public abstract class SshToolsApplicationApplet extends JApplet {
    //     eurgghh!

    /**  */
    public final static String[][] PARAMETER_INFO = {
        {
            "sshapps.log.file", "string",
            "Logging output destination. Defaults to @console@"
        },
        {
            "sshapps.log.level", "string",
            "Logging level. DEBUG,FATAL,ERROR,WARN,INFO,DEBUG or OFF. Defaults to OFF"
        },
        {
            "sshapps.ui.informationPanel.background", "hex color",
            "Set the background color of the 'information panel'"
        },
        {
            "sshapps.ui.informationPanel.foreground", "boolean",
            "Set the foreground color of the 'information panel'"
        },
        {
            "sshapps.ui.informationPanel.borderColor", "boolean",
            "Set the border color of the 'information panel'"
        },
        {
            "sshapps.ui.informationPanel.borderThickness", "integer",
            "Set the border thickness of the 'information panel'"
        },
        { "sshapps.ui.toolBar", "boolean", "Enable / Disable the tool bar" },
        { "sshapps.ui.menuBar", "boolean", "Enable / Disable the menu bar" },
        {
            "sshapps.ui.disabledActions", "string",
            "Comma (,) separated list of disable actions"
        },
        { "sshapps.ui.statusBar", "boolean", "Enable / Disable the menu bar" }
    };

    /**  */
    protected Log log = LogFactory.getLog(SshToolsApplicationApplet.class);

    //  Private instance variables

    /**  */
    protected LoadingPanel loadingPanel;

    /**  */
    protected JSeparator toolSeparator;

    /**  */
    protected SshToolsApplicationPanel applicationPanel;

    /**  */
    protected Color infoForeground;

    /**  */
    protected int infoBorderThickness;

    /**  */
    protected boolean toolBar;

    /**  */
    protected boolean menuBar;

    /**  */
    protected boolean statusBar;

    /**  */
    protected Color infoBackground;

    /**  */
    protected Color infoBorderColor;

    /**  */
    protected String disabledActions;

    /**
*
*
* @param key
* @param def
*
* @return
*/
    public String getParameter(String key, String def) {
        String v = getParameter(key);

        return (v != null) ? v : def;
    }

    /**
*
*/
    public void init() {
        try {
            Runnable r = new Runnable() {
                    public void run() {
                        try {
                            getContentPane().setLayout(new BorderLayout());
                            setAppletComponent(loadingPanel = new LoadingPanel());
                            initApplet();

                            JComponent p = buildAppletComponent();
                            startApplet();
                            setAppletComponent(p);
                        } catch (Throwable t) {
                            seriousAppletError(t);
                        }
                    }
                };

            Thread t = new Thread(r);
            t.start();
        } catch (Throwable t) {
            seriousAppletError(t);
        }
    }

    /**
*
*
* @throws IOException
*/
    public void initApplet() throws IOException {
        /*ConfigurationLoader.setLogfile(getParameter("sshapps.log.file",
"@console@"));
 log.getRootLogger().setLevel(org.apache.log4j.Level.toLevel(
getParameter("sshapps.log.level", "DEBUG")));*/
        ConfigurationLoader.initialize(false);
        infoBackground = PropertyUtil.stringToColor(getParameter(
                    "sshapps.ui.informationPanel.background",
                    PropertyUtil.colorToString(new Color(255, 255, 204))));
        infoForeground = PropertyUtil.stringToColor(getParameter(
                    "sshapps.ui.informationPanel.foreground",
                    PropertyUtil.colorToString(Color.black)));
        infoBorderColor = PropertyUtil.stringToColor(getParameter(
                    "sshapps.ui.informationPanel.borderColor",
                    PropertyUtil.colorToString(Color.black)));
        infoBorderThickness = PropertyUtil.stringToInt(getParameter(
                    "sshapps.ui.informationPanel.borderThickness", "1"), 1);
        toolBar = getParameter("sshapps.ui.toolBar", "true").equalsIgnoreCase("true");
        menuBar = getParameter("sshapps.ui.menuBar", "true").equalsIgnoreCase("true");
        statusBar = getParameter("sshapps.ui.statusBar", "true")
                        .equalsIgnoreCase("true");
        disabledActions = getParameter("sshapps.ui.disabledActions", "");
    }

    /**
*
*/
    public void startApplet() {
    }

    /**
*
*
* @return
*
* @throws IOException
* @throws SshToolsApplicationException
*/
    public JComponent buildAppletComponent()
        throws IOException, SshToolsApplicationException {
        loadingPanel.setStatus("Creating application");
        applicationPanel = createApplicationPanel();
        loadingPanel.setStatus("Building action components");
        applicationPanel.rebuildActionComponents();
        log.debug("Disabled actions list = " + disabledActions);

        StringTokenizer tk = new StringTokenizer((disabledActions == null) ? ""
                                                                           : disabledActions,
                ",");

        while (tk.hasMoreTokens()) {
            String n = tk.nextToken();
            log.debug("Disable " + n);
            applicationPanel.setActionVisible(n, false);
        }

        JPanel p = new JPanel(new BorderLayout());
        JPanel n = new JPanel(new BorderLayout());

        if (applicationPanel.getJMenuBar() != null) {
            n.add(applicationPanel.getJMenuBar(), BorderLayout.NORTH);
            log.debug("Setting menu bar visibility to " + menuBar);
            applicationPanel.setMenuBarVisible(menuBar);
        }

        if (applicationPanel.getToolBar() != null) {
            JPanel t = new JPanel(new BorderLayout());
            t.add(applicationPanel.getToolBar(), BorderLayout.NORTH);
            applicationPanel.setToolBarVisible(toolBar);
            t.add(toolSeparator = new JSeparator(JSeparator.HORIZONTAL),
                BorderLayout.SOUTH);
            toolSeparator.setVisible(applicationPanel.getToolBar().isVisible());

            final SshToolsApplicationPanel pnl = applicationPanel;
            applicationPanel.getToolBar().addComponentListener(new ComponentAdapter() {
                    public void componentHidden(ComponentEvent evt) {
                        toolSeparator.setVisible(pnl.getToolBar().isVisible());
                    }
                });
            n.add(t, BorderLayout.SOUTH);
        }

        p.add(n, BorderLayout.NORTH);
        p.add(applicationPanel, BorderLayout.CENTER);

        if (applicationPanel.getStatusBar() != null) {
            p.add(applicationPanel.getStatusBar(), BorderLayout.SOUTH);
            applicationPanel.setStatusBarVisible(statusBar);
        }

        return p;
    }

    /**
*
*
* @param name
*/
    public void doAction(String name) {
        StandardAction a = applicationPanel.getAction(name);

        if (a != null) {
            if (a.isEnabled()) {
                log.debug("Performing action " + a.getName());
                a.actionPerformed(new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED, a.getActionCommand()));
            } else {
                log.warn("No performing action '" + a.getName() +
                    "' because it is disabled.");
            }
        } else {
            log.error("No action named " + name);
        }
    }

    /**
*
*
* @return
*
* @throws SshToolsApplicationException
*/
    public abstract SshToolsApplicationPanel createApplicationPanel()
        throws SshToolsApplicationException;

    /**
*
*
* @param component
*/
    protected void setAppletComponent(JComponent component) {
        if (getContentPane().getComponentCount() > 0) {
            getContentPane().invalidate();
            getContentPane().removeAll();
        }

        getContentPane().add(component, BorderLayout.CENTER);
        getContentPane().validate();
        getContentPane().repaint();
    }

    /**
*
*
* @param t
*/
    protected void seriousAppletError(Throwable t) {
        StringBuffer buf = new StringBuffer();
        buf.append("<html><p>A serious error has occured ...</p><br>");
        buf.append("<p><font size=\"-1\" color=\"#ff0000\"><b>");

        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer, true));

        StringTokenizer tk = new StringTokenizer(writer.toString(), "\n");

        while (tk.hasMoreTokens()) {
            String msg = tk.nextToken();
            buf.append(msg);

            if (tk.hasMoreTokens()) {
                buf.append("<br>");
            }
        }

        buf.append("</b></font></p><html>");

        SshToolsApplicationAppletPanel p = new SshToolsApplicationAppletPanel();
        p.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.NONE;
        UIUtil.jGridBagAdd(p, new JLabel(buf.toString()), gbc,
            GridBagConstraints.REMAINDER);
        setAppletComponent(p);
    }

    /**
*
*/
    public void start() {
    }

    /**
*
*/
    public void stop() {
    }

    /**
*
*/
    public void destroy() {
    }

    /**
*
*
* @return
*/
    public String[][] getParameterInfo() {
        return PARAMETER_INFO;
    }

    public class SshToolsApplicationAppletContainer extends JPanel
        implements SshToolsApplicationContainer {
        //  Private instance variables
        private SshToolsApplicationPanel panel;
        private SshToolsApplication application;

        //Construct the applet
        public SshToolsApplicationAppletContainer() {
        }

        public void init(SshToolsApplication application,
            SshToolsApplicationPanel panel) throws SshToolsApplicationException {
            this.application = application;
            this.panel = panel;
            panel.registerActionMenu(new SshToolsApplicationPanel.ActionMenu(
                    "Help", "Help", 'h', 99));
            panel.registerAction(new AboutAction(this, application));
            getApplicationPanel().rebuildActionComponents();
        }

        public void setContainerTitle(String title) {
            getAppletContext().showStatus(title);
        }

        public SshToolsApplicationPanel getApplicationPanel() {
            return panel;
        }

        public void closeContainer() {
            //  We dont do anything here
        }

        public void setContainerVisible(boolean visible) {
            setVisible(visible);
        }

        public boolean isContainerVisible() {
            return isVisible();
        }
    }

    class SshToolsApplicationAppletPanel extends JPanel {
        SshToolsApplicationAppletPanel() {
            super();
            setOpaque(true);
            setBackground(infoBackground);
            setForeground(infoForeground);
            setBorder(BorderFactory.createLineBorder(infoBorderColor,
                    infoBorderThickness));
        }
    }

    class LoadingPanel extends SshToolsApplicationAppletPanel {
        private JProgressBar bar;

        LoadingPanel() {
            super();
            setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(0, 0, 8, 0);
            gbc.fill = GridBagConstraints.NONE;
            UIUtil.jGridBagAdd(this, new JLabel("Loading " + getAppletInfo()),
                gbc, GridBagConstraints.REMAINDER);
            bar = new JProgressBar(0, 100);

            //bar.setIndeterminate(true);
            bar.setStringPainted(true);
            UIUtil.jGridBagAdd(this, bar, gbc, GridBagConstraints.REMAINDER);
        }

        public void setStatus(String status) {
            bar.setString(status);
        }
    }
}
