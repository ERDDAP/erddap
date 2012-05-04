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

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class HostsTab extends JPanel implements OptionsTab, ActionListener {
    /**  */
    public final static String GLOBAL_ICON = "/com/sshtools/common/ui/largeserveridentity.png";

    /**  */
    public final static String ALLOW_ICON = "/com/sshtools/common/ui/ok.png";

    /**  */
    public final static String DENY_ICON = "/com/sshtools/common/ui/cancel.png";

    /**  */
    public final static String REMOVE_ICON = "/com/sshtools/common/ui/remove.png";
    private static Log log = LogFactory.getLog(HostsTab.class);

    //
    private JList hosts;
    private AbstractKnownHostsKeyVerification hostKeyVerifier;
    private JButton remove;

    //private JButton deny;
    private HostsListModel model;

    /**
* Creates a new HostsTab object.
*
* @param hostKeyVerifier
*/
    public HostsTab(AbstractKnownHostsKeyVerification hostKeyVerifier) {
        super();
        this.hostKeyVerifier = hostKeyVerifier;
        hosts = new JList(model = new HostsListModel());
        hosts.setVisibleRowCount(10);
        hosts.setCellRenderer(new HostRenderer());
        hosts.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent evt) {
                    setAvailableActions();
                }
            });
        remove = new JButton("Remove", new ResourceIcon(REMOVE_ICON));
        remove.addActionListener(this);

        //deny = new JButton("Deny", new ResourceIcon(DENY_ICON));
        //deny.addActionListener(this);
        JPanel b = new JPanel(new GridBagLayout());
        b.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1.0;
        UIUtil.jGridBagAdd(b, remove, gbc, GridBagConstraints.REMAINDER);
        gbc.weighty = 1.0;

        //UIUtil.jGridBagAdd(b, deny, gbc, GridBagConstraints.REMAINDER);
        JPanel s = new JPanel(new BorderLayout());
        s.add(new JScrollPane(hosts), BorderLayout.CENTER);
        s.add(b, BorderLayout.EAST);

        IconWrapperPanel w = new IconWrapperPanel(new ResourceIcon(GLOBAL_ICON),
                s);

        //  This tab
        setLayout(new BorderLayout());
        add(w, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        reset();
    }

    /**
*
*
* @param evt
*/
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == remove) {
            model.removeHostAt(hosts.getSelectedIndex());
        }

        /*else if (evt.getSource() == deny) {
int i = hosts.getSelectedIndex();
HostWrapper w = model.getHostAt(i);
w.allow = false;
model.updateHostAt(i);
 }*/
        setAvailableActions();
    }

    private void setAvailableActions() {
        HostWrapper w = ((model.getSize() > 0) &&
            (hosts.getSelectedValues().length == 1))
            ? model.getHostAt(hosts.getSelectedIndex()) : null;
        remove.setEnabled(w != null);

        //deny.setEnabled( (w != null) && w.allow);
    }

    /**
*
*/
    public void reset() {
        ((HostsListModel) hosts.getModel()).refresh();
        setAvailableActions();
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
        return "Hosts";
    }

    /**
*
*
* @return
*/
    public String getTabToolTipText() {
        return "Allowed and denied hosts.";
    }

    /**
*
*
* @return
*/
    public int getTabMnemonic() {
        return 'h';
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
        try {
            Map map = hostKeyVerifier.allowedHosts();
            String[] hosts = new String[map.keySet().size()];
            map.keySet().toArray(hosts);
            log.debug("Checking if any allowed hosts need to be removed");

            for (int i = hosts.length - 1; i >= 0; i--) {
                if (log.isDebugEnabled()) {
                    log.debug("Looking for host " + hosts[i]);
                }

                HostWrapper w = model.getHost(hosts[i]);

                if (w != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found host " + hosts[i]);
                    }

                    if (!w.allow) {
                        if (log.isDebugEnabled()) {
                            log.debug("Denying host " + hosts[i]);
                        }

                        hostKeyVerifier.removeAllowedHost(hosts[i]);

                        //hostKeyVerifier.denyHost(hosts[i], true);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Host removed " + hosts[i]);
                    }

                    hostKeyVerifier.removeAllowedHost(hosts[i]);
                }
            }

            /*java.util.List list = hostKeyVerifier.deniedHosts();
log.debug("Checking if any denied hosts need to be removed");
 for (int i = list.size() - 1; i >= 0; i--) {
String h = (String) list.get(i);
if (log.isDebugEnabled()) {
log.debug("Looking for host " + h);
}
HostWrapper w = model.getHost(h);
if (w == null) {
if (log.isDebugEnabled()) {
log.debug("Removing host " + h);
}
hostKeyVerifier.removeDeniedHost(h);
}
 }*/
            hostKeyVerifier.saveHostFile();
        } catch (InvalidHostFileException ihfe) {
            log.error("Failed to store hosts file.", ihfe);
        }
    }

    /**
*
*/
    public void tabSelected() {
    }

    class HostRenderer extends DefaultListCellRenderer {
        Icon allowIcon;
        Icon denyIcon;

        public HostRenderer() {
            allowIcon = new ResourceIcon(ALLOW_ICON);
            denyIcon = new ResourceIcon(DENY_ICON);
        }

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);

            HostWrapper w = (HostWrapper) value;
            setIcon(w.allow ? allowIcon : denyIcon);
            setText(w.host);

            return this;
        }
    }

    class HostWrapper {
        boolean allow;
        String host;
        SshPublicKey key;
        Map keys;

        HostWrapper(boolean allow, String host, Map keys) {
            this.allow = allow;
            this.host = host;
            this.keys = keys;
        }
    }

    class HostsListModel extends AbstractListModel {
        java.util.List hosts;

        public HostsListModel() {
            hosts = new java.util.ArrayList();
            refresh();
        }

        public void refresh() {
            hosts.clear();

            Map map = hostKeyVerifier.allowedHosts();

            for (Iterator i = map.keySet().iterator(); i.hasNext();) {
                String k = (String) i.next();
                Map keys = (Map) map.get(k);
                hosts.add(new HostWrapper(true, k, keys));
            }

            /* java.util.List list = hostKeyVerifier.deniedHosts();
for (Iterator i = list.iterator(); i.hasNext(); ) {
String h = (String) i.next();
hosts.add(new HostWrapper(false, h, null));
}*/
            fireContentsChanged(this, 0, getSize() - 1);
        }

        public HostWrapper getHost(String name) {
            for (Iterator i = hosts.iterator(); i.hasNext();) {
                HostWrapper w = (HostWrapper) i.next();

                if (w.host.equals(name)) {
                    return w;
                }
            }

            return null;
        }

        public int getSize() {
            return hosts.size();
        }

        public Object getElementAt(int index) {
            return hosts.get(index);
        }

        public HostWrapper getHostAt(int index) {
            return (HostWrapper) hosts.get(index);
        }

        public void removeHostAt(int index) {
            hosts.remove(index);
            fireIntervalRemoved(this, index, index);
        }

        public void updateHostAt(int index) {
            fireContentsChanged(this, index, index);
        }
    }
}
