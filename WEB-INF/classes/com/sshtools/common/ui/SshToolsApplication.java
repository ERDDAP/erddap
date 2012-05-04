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

import com.sshtools.common.mru.MRUList;
import com.sshtools.common.mru.MRUListModel;
import com.sshtools.common.util.BrowserLauncher;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.io.IOUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.security.AccessControlException;
import java.security.AccessController;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;


/**
 * An abstract application class that provides container management, look
 * and feel configuration and most recently used menus.
 *
 * @author Brett Smith
 * @version $Revision: 1.19 $
 */
public abstract class SshToolsApplication {
    /**  */
    public final static String PREF_CONNECTION_LAST_HOST = "apps.connection.lastHost";

    /**  */
    public final static String PREF_CONNECTION_LAST_USER = "apps.connection.lastUser";

    /**  */
    public final static String PREF_CONNECTION_LAST_PORT = "apps.connection.lastPort";

    /**  */
    public final static String PREF_CONNECTION_LAST_KEY = "apps.connection.lastKey";

    /**  */
    public final static String PREF_LAF = "apps.laf";

    /**  */
    public final static String CROSS_PLATFORM_LAF = "CROSS_PLATFORM";

    /**  */
    public final static String DEFAULT_LAF = "DEFAULT";

    /**  */
    public final static String SYSTEM_LAF = "SYSTEM";

    /**  */
    protected static Vector containers = new Vector();

    /**  */
    protected static Log log = LogFactory.getLog(SshToolsApplication.class);

    /**  */
    protected static MRUListModel mruModel;
    private static UIManager.LookAndFeelInfo[] allLookAndFeelInfo;

    static {
        UIManager.LookAndFeelInfo[] i;

        try {
            i = UIManager.getInstalledLookAndFeels();
        } catch (Throwable t) {
            i = new UIManager.LookAndFeelInfo[0];
        }

        allLookAndFeelInfo = new UIManager.LookAndFeelInfo[i.length + 3];
        System.arraycopy(i, 0, allLookAndFeelInfo, 0, i.length);
        allLookAndFeelInfo[i.length] = new UIManager.LookAndFeelInfo("Default",
                DEFAULT_LAF);

        allLookAndFeelInfo[i.length + 1] = new UIManager.LookAndFeelInfo("Cross Platform",
                CROSS_PLATFORM_LAF);
        allLookAndFeelInfo[i.length + 2] = new UIManager.LookAndFeelInfo("System",
                SYSTEM_LAF);
    }

    /**  */
    protected Class panelClass;

    /**  */
    protected Class defaultContainerClass;

    /**  */
    protected java.util.List additionalOptionsTabs;

    /**
* Creates a new SshToolsApplication object.
*
* @param panelClass
* @param defaultContainerClass
*/
    public SshToolsApplication(Class panelClass, Class defaultContainerClass) {
        this.panelClass = panelClass;
        this.defaultContainerClass = defaultContainerClass;
        additionalOptionsTabs = new java.util.ArrayList();

        try {
            if (System.getSecurityManager() != null) {
                AccessController.checkPermission(new FilePermission(
                        "<<ALL FILES>>", "write"));
            }

            File a = getApplicationPreferencesDirectory();

            if (a == null) {
                throw new AccessControlException(
                    "Application preferences directory not specified.");
            }

            InputStream in = null;
            MRUList mru = new MRUList();

            try {
                File f = new File(a, getApplicationName() + ".mru");

                if (f.exists()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Loading MRU from " + f.getAbsolutePath());
                    }

                    in = new FileInputStream(f);
                    mru.reload(in);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("MRU file " + f.getAbsolutePath() +
                            " doesn't exist, creating empty list");
                    }
                }
            } catch (Exception e) {
                log.error("Could not load MRU list.", e);
            } finally {
                IOUtil.closeStream(in);
            }

            mruModel = new MRUListModel();
            mruModel.setMRUList(mru);
        } catch (AccessControlException ace) {
            log.error("Could not load MRU.", ace);
        }
    }

    /**
*
*
* @return
*/
    public static UIManager.LookAndFeelInfo[] getAllLookAndFeelInfo() {
        return allLookAndFeelInfo;
    }

    /**
*
*
* @return
*/
    public MRUListModel getMRUModel() {
        return mruModel;
    }

    /**
*
*
* @return
*/
    public abstract String getApplicationName();

    /**
*
*
* @return
*/
    public abstract String getApplicationVersion();

    /**
*
*
* @return
*/
    public abstract Icon getApplicationLargeIcon();

    /**
*
*
* @return
*/
    public abstract String getAboutLicenseDetails();

    /**
*
*
* @return
*/
    public abstract String getAboutURL();

    /**
*
*
* @return
*/
    public abstract String getAboutAuthors();

    /**
*
*
* @return
*/
    public abstract File getApplicationPreferencesDirectory();

    /**
*
*
* @return
*/
    public OptionsTab[] getAdditionalOptionsTabs() {
        OptionsTab[] t = new OptionsTab[additionalOptionsTabs.size()];
        additionalOptionsTabs.toArray(t);

        return t;
    }

    /**
*
*
* @param tab
*/
    public void addAdditionalOptionsTab(OptionsTab tab) {
        if (!additionalOptionsTabs.contains(tab)) {
            additionalOptionsTabs.add(tab);
        }
    }

    /**
*
*
* @param tab
*/
    public void removeAdditionalOptionsTab(OptionsTab tab) {
        additionalOptionsTabs.remove(tab);
    }

    /**
*
*
* @param title
*/
    public void removeAdditionalOptionsTab(String title) {
        OptionsTab t = getOptionsTab(title);

        if (t != null) {
            removeAdditionalOptionsTab(t);
        }
    }

    /**
*
*
* @param title
*
* @return
*/
    public OptionsTab getOptionsTab(String title) {
        for (Iterator i = additionalOptionsTabs.iterator(); i.hasNext();) {
            OptionsTab t = (OptionsTab) i.next();

            if (t.getTabTitle().equals(title)) {
                return t;
            }
        }

        return null;
    }

    /**
*
*/
    public void exit() {
        log.debug("Exiting application");
        PreferencesStore.savePreferences();

        FileOutputStream out = null;
        File a = getApplicationPreferencesDirectory();

        if (a != null) {
            try {
                File f = new File(getApplicationPreferencesDirectory(),
                        getApplicationName() + ".mru");
                ;

                if (log.isDebugEnabled()) {
                    log.debug("Saving MRU to " + f.getAbsolutePath());
                }

                out = new FileOutputStream(f);

                PrintWriter w = new PrintWriter(out, true);
                w.println(mruModel.getMRUList().toString());
            } catch (IOException ioe) {
                log.error("Could not save MRU. ", ioe);
            } finally {
                IOUtil.closeStream(out);
            }
        } else {
            log.debug(
                "Not saving preferences because no preferences directory is available.");
        }

        System.exit(0);
    }

    /**
*
*
* @return
*/
    public int getContainerCount() {
        return containers.size();
    }

    /**
*
*
* @param idx
*
* @return
*/
    public SshToolsApplicationContainer getContainerAt(int idx) {
        return (SshToolsApplicationContainer) containers.elementAt(idx);
    }

    /**
*
*
* @param panel
*
* @return
*/
    public SshToolsApplicationContainer getContainerForPanel(
        SshToolsApplicationPanel panel) {
        for (Iterator i = containers.iterator(); i.hasNext();) {
            SshToolsApplicationContainer c = (SshToolsApplicationContainer) i.next();

            if (c.getApplicationPanel() == panel) {
                return c;
            }
        }

        return null;
    }

    /**
*
*
* @param container
*/
    public void closeContainer(SshToolsApplicationContainer container) {
        if (log.isDebugEnabled()) {
            log.debug("Asking " + container + " if it can close");
        }

        if (container.getApplicationPanel().canClose()) {
            if (log.isDebugEnabled()) {
                log.debug("Closing");

                for (Iterator i = containers.iterator(); i.hasNext();) {
                    log.debug(i.next() + " is currently open");
                }
            }

            container.getApplicationPanel().close();
            container.closeContainer();
            containers.removeElement(container);

            if (containers.size() == 0) {
                exit();
            } else {
                log.debug(
                    "Not closing completely because there are containers still open");

                for (Iterator i = containers.iterator(); i.hasNext();) {
                    log.debug(i.next() + " is still open");
                }
            }
        }
    }

    /**
* Show an 'About' dialog
*
*
*/
    public void showAbout(Component parent) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        GridBagConstraints gBC = new GridBagConstraints();
        gBC.anchor = GridBagConstraints.CENTER;
        gBC.fill = GridBagConstraints.HORIZONTAL;
        gBC.insets = new Insets(1, 1, 1, 1);

        JLabel a = new JLabel(getApplicationName());
        a.setFont(a.getFont().deriveFont(24f));
        UIUtil.jGridBagAdd(p, a, gBC, GridBagConstraints.REMAINDER);

        JLabel v = new JLabel(ConfigurationLoader.getVersionString(
                    getApplicationName(), getApplicationVersion()));
        v.setFont(v.getFont().deriveFont(10f));
        UIUtil.jGridBagAdd(p, v, gBC, GridBagConstraints.REMAINDER);

        MultilineLabel x = new MultilineLabel(getAboutAuthors());
        x.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        x.setFont(x.getFont().deriveFont(12f));
        UIUtil.jGridBagAdd(p, x, gBC, GridBagConstraints.REMAINDER);

        MultilineLabel c = new MultilineLabel(getAboutLicenseDetails());
        c.setFont(c.getFont().deriveFont(10f));
        UIUtil.jGridBagAdd(p, c, gBC, GridBagConstraints.REMAINDER);

        final JLabel h = new JLabel(getAboutURL());
        h.setForeground(Color.blue);
        h.setFont(new Font(h.getFont().getName(), Font.BOLD, 10));
        h.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        h.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    try {
                        BrowserLauncher.openURL(getAboutURL());
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });
        UIUtil.jGridBagAdd(p, h, gBC, GridBagConstraints.REMAINDER);
        JOptionPane.showMessageDialog(parent, p, "About",
            JOptionPane.PLAIN_MESSAGE, getApplicationLargeIcon());
    }

    /**
*
*
* @return
*
* @throws SshToolsApplicationException
*/
    public SshToolsApplicationContainer newContainer()
        throws SshToolsApplicationException {
        SshToolsApplicationContainer container = null;

        try {
            container = (SshToolsApplicationContainer) defaultContainerClass.newInstance();
            newContainer(container);

            return container;
        } catch (Throwable t) {
            throw new SshToolsApplicationException(t);
        }
    }

    /**
*
*
* @param container
*
* @throws SshToolsApplicationException
*/
    public void newContainer(SshToolsApplicationContainer container)
        throws SshToolsApplicationException {
        try {
            SshToolsApplicationPanel panel = (SshToolsApplicationPanel) panelClass.newInstance();
            panel.init(this);
            panel.rebuildActionComponents();
            panel.setAvailableActions();
            container.init(this, panel);
            panel.setContainer(container);

            if (!container.isContainerVisible()) {
                container.setContainerVisible(true);
            }

            containers.addElement(container);
        } catch (Throwable t) {
            throw new SshToolsApplicationException(t);
        }
    }

    /**
*
*
* @param container
* @param newContainerClass
*
* @return
*
* @throws SshToolsApplicationException
*/
    public SshToolsApplicationContainer convertContainer(
        SshToolsApplicationContainer container, Class newContainerClass)
        throws SshToolsApplicationException {
        log.info("Converting container of class " +
            container.getClass().getName() + " to " +
            newContainerClass.getName());

        int idx = containers.indexOf(container);

        if (idx == -1) {
            throw new SshToolsApplicationException(
                "Container is not being manager by the application.");
        }

        SshToolsApplicationContainer newContainer = null;

        try {
            container.closeContainer();

            SshToolsApplicationPanel panel = container.getApplicationPanel();
            newContainer = (SshToolsApplicationContainer) newContainerClass.newInstance();
            newContainer.init(this, panel);
            panel.setContainer(newContainer);

            if (!newContainer.isContainerVisible()) {
                newContainer.setContainerVisible(true);
            }

            containers.setElementAt(newContainer, idx);

            return newContainer;
        } catch (Throwable t) {
            throw new SshToolsApplicationException(t);
        }
    }

    /**
*
*
* @param args
*
* @throws SshToolsApplicationException
*/
    public void init(String[] args) throws SshToolsApplicationException {
        File f = getApplicationPreferencesDirectory();

        if (f != null) {
            //
            PreferencesStore.init(new File(f,
                    getApplicationName() + ".properties"));
            log.info("Preferences will be saved to " + f.getAbsolutePath());
        } else {
            log.warn("No preferences can be saved.");
        }

        try {
            setLookAndFeel(PreferencesStore.get(PREF_LAF, SYSTEM_LAF));
            UIManager.put("OptionPane.errorIcon",
                new ResourceIcon(SshToolsApplication.class, "dialog-error4.png"));
            UIManager.put("OptionPane.informationIcon",
                new ResourceIcon(SshToolsApplication.class,
                    "dialog-information.png"));
            UIManager.put("OptionPane.warningIcon",
                new ResourceIcon(SshToolsApplication.class,
                    "dialog-warning2.png"));
            UIManager.put("OptionPane.questionIcon",
                new ResourceIcon(SshToolsApplication.class,
                    "dialog-question3.png"));
        } catch (Throwable t) {
            log.error(t);
        }
    }

    /**
*
*
* @param className
*
* @throws Exception
*/
    public static void setLookAndFeel(String className)
        throws Exception {
        LookAndFeel laf = null;

        if (!className.equals(DEFAULT_LAF)) {
            if (className.equals(SYSTEM_LAF)) {
                String systemLaf = UIManager.getSystemLookAndFeelClassName();
                log.debug("System Look And Feel is " + systemLaf);
                laf = (LookAndFeel) Class.forName(systemLaf).newInstance();
            } else if (className.equals(CROSS_PLATFORM_LAF)) {
                String crossPlatformLaf = UIManager.getCrossPlatformLookAndFeelClassName();
                log.debug("Cross Platform Look And Feel is " +
                    crossPlatformLaf);
                laf = (LookAndFeel) Class.forName(crossPlatformLaf).newInstance();
            } else {
                laf = (LookAndFeel) Class.forName(className).newInstance();
            }
        }

        //  Now actually set the look and feel
        if (laf != null) {
            log.info("Setting look and feel " + laf.getName() + " (" +
                laf.getClass().getName() + ")");
            UIManager.setLookAndFeel(laf);
            UIManager.put("EditorPane.font", UIManager.getFont("TextArea.font"));
        }
    }
}
