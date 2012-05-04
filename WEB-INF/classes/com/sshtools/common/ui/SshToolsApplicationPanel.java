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

import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.23 $
 */
public abstract class SshToolsApplicationPanel extends JPanel {
    //

    /**  */
    protected Log log = LogFactory.getLog(SshToolsApplicationPanel.class);

    /**  */
    protected SshToolsApplication application;

    /**  */
    protected JMenuBar menuBar;

    /**  */
    protected JToolBar toolBar;

    /**  */
    protected JPopupMenu contextMenu;

    /**  */
    protected SshToolsApplicationContainer container;

    /**  */
    protected Vector actions = new Vector();

    /**  */
    protected HashMap actionsVisible = new HashMap();

    /**  */
    protected boolean toolsVisible;

    /**  */
    protected Vector actionMenus = new Vector();

    /**
* Creates a new SshToolsApplicationPanel object.
*/
    public SshToolsApplicationPanel() {
        super();
    }

    /**
* Creates a new SshToolsApplicationPanel object.
*
* @param mgr
*/
    public SshToolsApplicationPanel(LayoutManager mgr) {
        super(mgr);
    }

    /**
* Called by the application framework to test the closing state
*
* @return
*/
    public abstract boolean canClose();

    /**
* Called by the application framework to close the panel
*/
    public abstract void close();

    /**
* Called by the application framework when a change in connection state
* has occured. The available actions should be enabled/disabled in this
* methods implementation
*/
    public abstract void setAvailableActions();

    /**
* Set an actions visible state
*
* @param name
* @param visible
*/
    public void setActionVisible(String name, boolean visible) {
        log.debug("Setting action '" + name + "' to visibility " + visible);
        actionsVisible.put(name, new Boolean(visible));
    }

    /**
* Gets the container for this panel.
*
* @return
*/
    public SshToolsApplicationContainer getContainer() {
        return container;
    }

    /**
* Sets the container for this panel
*
* @param container
*/
    public void setContainer(SshToolsApplicationContainer container) {
        this.container = container;
    }

    /**
* Register a new menu
*
* @param actionMenu
*/
    public void registerActionMenu(ActionMenu actionMenu) {
        ActionMenu current = getActionMenu(actionMenu.name);

        if (current == null) {
            actionMenus.addElement(actionMenu);
        }
    }

    /**
* Gets a menu by name
*
* @param actionMenuName
*
* @return
*/
    public ActionMenu getActionMenu(String actionMenuName) {
        return getActionMenu(actionMenus.iterator(), actionMenuName);
    }

    private ActionMenu getActionMenu(Iterator actions, String actionMenuName) {
        while (actions.hasNext()) {
            ActionMenu a = (ActionMenu) actions.next();

            if (a.name.equals(actionMenuName)) {
                return a;
            }
        }

        return null;
    }

    /**
* Get an action by name
*
* @param name
*
* @return
*/
    public StandardAction getAction(String name) {
        for (Iterator i = actions.iterator(); i.hasNext();) {
            StandardAction a = (StandardAction) i.next();

            if (a.getName().equals(name)) {
                return a;
            }
        }

        return null;
    }

    /**
* Deregister an action
*
* @param action
*/
    public void deregisterAction(StandardAction action) {
        actions.removeElement(action);
    }

    /**
* Register a new action
*
* @param action
*/
    public void registerAction(StandardAction action) {
        actions.addElement(action);
    }

    /**
* Initialize the panel
*
* @param application
*
* @throws SshToolsApplicationException
*/
    public void init(SshToolsApplication application)
        throws SshToolsApplicationException {
        this.application = application;
        menuBar = new JMenuBar();

        // Creat the tool bar
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        // Create the context menu
        contextMenu = new JPopupMenu();
        registerActionMenu(new ActionMenu("Tools", "Tools", 't', 30));

        if (PreferencesStore.isStoreAvailable()) {
            log.debug("Preferences store is available, adding options action");
            registerAction(new OptionsAction() {
                    public void actionPerformed(ActionEvent evt) {
                        showOptions();
                    }
                });
        }
    }

    /**
* Show the options dialog
*/
    public void showOptions() {
        OptionsTab[] tabs = getApplication().getAdditionalOptionsTabs();
        OptionsPanel.showOptionsDialog(this, tabs);
    }

    /**
* Rebuild all the action components such as toobar, context menu
*/
    public void rebuildActionComponents() {
        //  Clear the current state of the component
        log.debug("Rebuild action components");
        toolBar.removeAll();

        //
        Vector enabledActions = new Vector();

        for (Iterator i = actions.iterator(); i.hasNext();) {
            StandardAction a = (StandardAction) i.next();
            String n = (String) a.getValue(Action.NAME);
            Boolean s = (Boolean) actionsVisible.get(n);

            if (s == null) {
                s = Boolean.TRUE;
            }

            if (Boolean.TRUE.equals(s)) {
                log.debug("Action " + n + " is enabled.");
                enabledActions.add(a);
            } else {
                log.debug("Action " + n + " not enabled.");
            }
        }

        //  Build the tool bar, grouping the actions
        Vector v = new Vector();

        for (Iterator i = enabledActions.iterator(); i.hasNext();) {
            StandardAction a = (StandardAction) i.next();

            if (Boolean.TRUE.equals(
                        (Boolean) a.getValue(StandardAction.ON_TOOLBAR))) {
                v.addElement(a);
            }
        }

        Collections.sort(v, new ToolBarActionComparator());

        Integer grp = null;

        for (Iterator i = v.iterator(); i.hasNext();) {
            StandardAction z = (StandardAction) i.next();

            if ((grp != null) &&
                    !grp.equals(
                        (Integer) z.getValue(StandardAction.TOOLBAR_GROUP))) {
                toolBar.add(new ToolBarSeparator());
            }

            if (Boolean.TRUE.equals(
                        (Boolean) z.getValue(StandardAction.IS_TOGGLE_BUTTON))) {
                ToolToggleButton tBtn = new ToolToggleButton(z);
                toolBar.add(tBtn);
            } else {
                ToolButton btn = new ToolButton(z);
                toolBar.add(btn);
            }

            grp = (Integer) z.getValue(StandardAction.TOOLBAR_GROUP);
        }

        toolBar.revalidate();
        toolBar.repaint();

        //  Build the context menu, grouping the actions
        Vector c = new Vector();
        contextMenu.removeAll();

        for (Iterator i = enabledActions.iterator(); i.hasNext();) {
            StandardAction a = (StandardAction) i.next();

            if (Boolean.TRUE.equals(
                        (Boolean) a.getValue(StandardAction.ON_CONTEXT_MENU))) {
                c.addElement(a);
            }
        }

        Collections.sort(c, new ContextActionComparator());
        grp = null;

        for (Iterator i = c.iterator(); i.hasNext();) {
            StandardAction z = (StandardAction) i.next();

            if ((grp != null) &&
                    !grp.equals(
                        (Integer) z.getValue(StandardAction.CONTEXT_MENU_GROUP))) {
                contextMenu.addSeparator();
            }

            contextMenu.add(z);
            grp = (Integer) z.getValue(StandardAction.CONTEXT_MENU_GROUP);
        }

        contextMenu.revalidate();

        //  Build the menu bar
        menuBar.removeAll();
        v.removeAllElements();

        for (Enumeration e = enabledActions.elements(); e.hasMoreElements();) {
            StandardAction a = (StandardAction) e.nextElement();

            if (Boolean.TRUE.equals(
                        (Boolean) a.getValue(StandardAction.ON_MENUBAR))) {
                v.addElement(a);
            }
        }

        Vector menus = (Vector) actionMenus.clone();
        Collections.sort(menus);

        HashMap map = new HashMap();

        for (Iterator i = v.iterator(); i.hasNext();) {
            StandardAction z = (StandardAction) i.next();
            String menuName = (String) z.getValue(StandardAction.MENU_NAME);

            if (menuName == null) {
                log.error("Action " + z.getName() +
                    " doesnt specify a value for " + StandardAction.MENU_NAME);
            } else {
                String m = (String) z.getValue(StandardAction.MENU_NAME);
                ActionMenu menu = getActionMenu(menus.iterator(), m);

                if (menu == null) {
                    log.error("Action menu " + z.getName() + " does not exist");
                } else {
                    Vector x = (Vector) map.get(menu.name);

                    if (x == null) {
                        x = new Vector();
                        map.put(menu.name, x);
                    }

                    x.addElement(z);
                }
            }
        }

        for (Iterator i = menus.iterator(); i.hasNext();) {
            ActionMenu m = (ActionMenu) i.next();
            Vector x = (Vector) map.get(m.name);

            if (x != null) {
                Collections.sort(x, new MenuItemActionComparator());

                JMenu menu = new JMenu(m.displayName);
                menu.setMnemonic(m.mnemonic);
                grp = null;

                for (Iterator j = x.iterator(); j.hasNext();) {
                    StandardAction a = (StandardAction) j.next();
                    Integer g = (Integer) a.getValue(StandardAction.MENU_ITEM_GROUP);

                    if ((grp != null) && !g.equals(grp)) {
                        menu.addSeparator();
                    }

                    grp = g;

                    if (a instanceof MenuAction) {
                        JMenu mnu = (JMenu) a.getValue(MenuAction.MENU);
                        menu.add(mnu);
                    } else {
                        JMenuItem item = new JMenuItem(a);
                        menu.add(item);
                    }
                }

                menuBar.add(menu);
            } else {
                log.error("Can't find menu " + m.name);
            }
        }

        menuBar.validate();
        menuBar.repaint();
    }

    /**
* Determine if the toolbar, menu and statusbar are visible
*
* @return
*/
    public boolean isToolsVisible() {
        return toolsVisible;
    }

    // Adds the new favorite to the appropriate favorite menu
    public void addFavorite(StandardAction action) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);

            if ((menu.getText() != null) && menu.getText().equals("Favorites")) {
                menu.add(action);
            }
        }
    }

    /**
* Set the visible state of the menu bar
*
* @param visible
*/
    public void setMenuBarVisible(boolean visible) {
        if ((getJMenuBar() != null) && (getJMenuBar().isVisible() != visible)) {
            getJMenuBar().setVisible(visible);
            revalidate();
        }
    }

    /**
* Set the visible state of the toolbar
*
* @param visible
*/
    public void setToolBarVisible(boolean visible) {
        if ((getToolBar() != null) && (getToolBar().isVisible() != visible)) {
            getToolBar().setVisible(visible);
            revalidate();
        }
    }

    /**
* Set the visible state of the statusbar
*
* @param visible
*/
    public void setStatusBarVisible(boolean visible) {
        if ((getStatusBar() != null) &&
                (getStatusBar().isVisible() != visible)) {
            getStatusBar().setVisible(visible);
            revalidate();
        }
    }

    /**
* Set the visible state of all tools. This will set the toolbar, menu and
* status bar visible states to the value provided.
*
* @param visible
*/
    public void setToolsVisible(boolean visible) {
        synchronized (getTreeLock()) {
            if ((getToolBar() != null) &&
                    (getToolBar().isVisible() != visible)) {
                getToolBar().setVisible(visible);
            }

            if ((getJMenuBar() != null) &&
                    (getJMenuBar().isVisible() != visible)) {
                getJMenuBar().setVisible(visible);
            }

            if ((getStatusBar() != null) &&
                    (getStatusBar().isVisible() != visible)) {
                getStatusBar().setVisible(visible);
            }

            toolsVisible = visible;
            revalidate();
        }
    }

    /**
* Show an exception message
*
* @param title
* @param message
*/
    public void showExceptionMessage(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    /**
* Show an error message with detail
*
* @param parent
* @param title
* @param exception
*/
    public static void showErrorMessage(Component parent, String title,
        Throwable exception) {
        showErrorMessage(parent, null, title, exception);
    }

    /**
* Show an error message with toggable detail
*
* @param parent
* @param mesg
* @param title
* @param exception
*/
    public static void showErrorMessage(Component parent, String mesg,
        String title, Throwable exception) {
        boolean details = false;

        while (true) {
            String[] opts = new String[] {
                    details ? "Hide Details" : "Details", "Ok"
                };
            StringBuffer buf = new StringBuffer();

            if (mesg != null) {
                buf.append(mesg);
            }

            appendException(exception, 0, buf, details);

            MultilineLabel message = new MultilineLabel(buf.toString());
            int opt = JOptionPane.showOptionDialog(parent, message, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE,
                    null, opts, opts[1]);

            if (opt == 0) {
                details = !details;
            } else {
                break;
            }
        }
    }

    private static void appendException(Throwable exception, int level,
        StringBuffer buf, boolean details) {
        try {
            if (((exception != null) && (exception.getMessage() != null)) &&
                    (exception.getMessage().length() > 0)) {
                if (details && (level > 0)) {
                    buf.append("\n \nCaused by ...\n");
                }

                buf.append(exception.getMessage());
            }

            if (details) {
                if (exception != null) {
                    if ((exception.getMessage() != null) &&
                            (exception.getMessage().length() == 0)) {
                        buf.append("\n \nCaused by ...");
                    } else {
                        buf.append("\n \n");
                    }
                }

                StringWriter sw = new StringWriter();

                if (exception != null) {
                    exception.printStackTrace(new PrintWriter(sw));
                }

                buf.append(sw.toString());
            }

            try {
                java.lang.reflect.Method method = exception.getClass()
                                                           .getMethod("getCause",
                        new Class[] {  });
                Throwable cause = (Throwable) method.invoke(exception, null);

                if (cause != null) {
                    appendException(cause, level + 1, buf, details);
                }
            } catch (Exception e) {
            }
        } catch (Throwable ex) {
        }
    }

    /**
* Returns the connected state of the panel
*
* @return
*/
    public abstract boolean isConnected();

    /**
* Set the title of the container
*
* @param file
*/
    public void setContainerTitle(File file) {
        String verString = "";

        if (application != null) {
            verString = ConfigurationLoader.getVersionString(application.getApplicationName(),
                    application.getApplicationVersion());
        }

        if (container != null) {
            container.setContainerTitle((file == null) ? verString
                                                       : (verString + " [" +
                file.getName() + "]"));
        }
    }

    /**
* Gets the toolbar
*
* @return
*/
    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
* Get the context menu
*
* @return
*/
    public JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
* Get the main menu
*
* @return
*/
    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    /**
* Get the status bar
*
* @return
*/
    public StatusBar getStatusBar() {
        return null;
    }

    /**
* Get the application attached to the panel
*
* @return
*/
    public SshToolsApplication getApplication() {
        return application;
    }

    /**
* Get the icon for the panel
*
* @return
*/
    public abstract ResourceIcon getIcon();

    public static class ActionMenu implements Comparable {
        int weight;
        int mnemonic;
        String name;
        String displayName;

        public ActionMenu(String name, String displayName, int mnemonic,
            int weight) {
            this.name = name;
            this.displayName = displayName;
            this.mnemonic = mnemonic;
            this.weight = weight;
        }

        public int compareTo(Object o) {
            int i = new Integer(weight).compareTo(new Integer(
                        ((ActionMenu) o).weight));

            return (i == 0)
            ? displayName.compareTo(((ActionMenu) o).displayName) : i;
        }
    }

    class ToolBarActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i = ((Integer) ((StandardAction) o1).getValue(StandardAction.TOOLBAR_GROUP)).compareTo((Integer) ((StandardAction) o2).getValue(
                        StandardAction.TOOLBAR_GROUP));

            return (i == 0)
            ? ((Integer) ((StandardAction) o1).getValue(StandardAction.TOOLBAR_WEIGHT)).compareTo((Integer) ((StandardAction) o2).getValue(
                    StandardAction.TOOLBAR_WEIGHT)) : i;
        }
    }

    class ContextActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i = ((Integer) ((StandardAction) o1).getValue(StandardAction.CONTEXT_MENU_GROUP)).compareTo((Integer) ((StandardAction) o2).getValue(
                        StandardAction.CONTEXT_MENU_GROUP));

            return (i == 0)
            ? ((Integer) ((StandardAction) o1).getValue(StandardAction.CONTEXT_MENU_WEIGHT)).compareTo((Integer) ((StandardAction) o2).getValue(
                    StandardAction.CONTEXT_MENU_WEIGHT)) : i;
        }
    }

    class MenuItemActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i = ((Integer) ((StandardAction) o1).getValue(StandardAction.MENU_ITEM_GROUP)).compareTo((Integer) ((StandardAction) o2).getValue(
                        StandardAction.MENU_ITEM_GROUP));

            return (i == 0)
            ? ((Integer) ((StandardAction) o1).getValue(StandardAction.MENU_ITEM_WEIGHT)).compareTo((Integer) ((StandardAction) o2).getValue(
                    StandardAction.MENU_ITEM_WEIGHT)) : i;
        }
    }

    class ConnectionFileFilter extends javax.swing.filechooser.FileFilter {
        public boolean accept(File f) {
            return f.isDirectory() ||
            f.getName().toLowerCase().endsWith(".xml");
        }

        public String getDescription() {
            return "Connection files (*.xml)";
        }
    }
}
