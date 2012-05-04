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

import com.sshtools.common.configuration.SshToolsConnectionProfile;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.SshEventAdapter;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelFactory;
import com.sshtools.j2ssh.forwarding.ForwardingClient;
import com.sshtools.j2ssh.session.SessionChannelClient;

import java.awt.BorderLayout;

import java.io.IOException;


/**
 * <p>This frame class embeds a SessionProvider and manages the connection
 * on behalf of the caller. To invoke a session provider from an external
 * application is a straight forward procedure. Assuming that the connection
 * has already been established [see SshClient] you can invoke a frame using
 * the following code:</p>
 *
 * <blockquote><pre>
 * // Create an SshClient connection
 * SshClient ssh = new SshClient();
 *
 * // Connection code goes here - see SshClient for more details
 *
 * SessionProviderFrame frame = new SessionProviderFrame(null,
 *                            new SshToolsConnectionProfile(),
 *                                                        ssh,
 *          SessionProviderFactory.getInstance().getProvider("sshterm"));
 * frame.pack();
 * frame.show();
 * </pre></blockquote>
 *
 * @author Lee David Painter
 * @version $Id: SessionProviderFrame.java,v 1.9 2003/11/16 19:30:08 rpernavas Exp $
 */
public class SessionProviderFrame extends SshToolsApplicationFrame
    implements SessionManager {
    //  Private instance variables
    private SshToolsApplicationSessionPanel panel;
    private SessionProvider provider;
    private SshToolsConnectionProfile profile;
    private SshClient ssh;
    private boolean disconnectOnClose = false;

    /**
* Construct a new Session Provider frame.
*
* @param app The SshToolsApplication instance, can be null
* @param profile The profile of the connection
* @param ssh the client connection
* @param provider the provider instance
* @throws IOException
* @throws SshToolsApplicationException
*/
    public SessionProviderFrame(SshToolsConnectionProfile profile,
        SshClient ssh, SessionProvider provider)
        throws IOException, SshToolsApplicationException {
        try {
            this.provider = provider;
            this.ssh = ssh;
            this.profile = profile;
            setIconImage(provider.getSmallIcon().getImage());
            setTitle(provider.getName() + " - " +
                ssh.getConnectionProperties().getHost());
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(panel = (SshToolsApplicationSessionPanel) provider.getProviderClass()
                                                                                   .newInstance(),
                BorderLayout.CENTER);

            return;
        } catch (IllegalAccessException ex) {
        } catch (InstantiationException ex) {
        }

        throw new SshToolsApplicationException("Failed to create instance of " +
            provider.getProviderClass().getName());
    }

    /**
* Initialize the frame and open the remote session
* @param app the application object, can be null
* @return
* @throws IOException
* @throws SshToolsApplicationException
*/
    public boolean initFrame(SshToolsApplication app)
        throws IOException, SshToolsApplicationException {
        panel.setCurrentConnectionProfile(profile);
        panel.init(app);
        init(app, panel);
        pack();

        return panel.openSession(this, profile);
    }

    /**
* Get the attached session provider panel.
* @return
*/
    public SshToolsApplicationSessionPanel getSessionPanel() {
        return panel;
    }

    /**
* Returns the guessed EOL setting of the remote computer
* @return
*/
    public int getRemoteEOL() {
        return ssh.getRemoteEOL();
    }

    /**
* Called by the application framework when testing exit state
* @return
*/
    public boolean canExit() {
        return panel.canClose();
    }

    /**
 * Called by the framework when exiting. Can also be called to close the session.
*/
    public void exit() {
        panel.close();
        dispose();
    }

    /**
* Implementation of the SessionManager method, simply calls the SshClient
* openSession method.
* @return
* @throws IOException
*/
    public SessionChannelClient openSession() throws IOException {
        return ssh.openSessionChannel();
    }

    /**
 * Implementation of the SessionManager method, this does nothing. Overide this
 * method to provide additional functionality to save changes made by the session
* to the profile.
*
* @param profile
*/
    public void applyProfileChanges(SshToolsConnectionProfile profile) {
    }

    /**
* When the session closes, should the connection be disconnected?
* @param disconnectOnClose
*/
    public void setDisconnectOnClose(boolean disconnectOnClose) {
        this.disconnectOnClose = disconnectOnClose;
    }

    /**
 * Implementation of the SessionManager method, this simply calls the SshClient
* method openSftpClient.
* @return
* @throws IOException
*/
    public SftpClient openSftpClient() throws IOException {
        return ssh.openSftpClient();
    }

    /**
 * Implementation of the SessionManager method, this simply calls the SshClient
* method openChannel.
* @param channel
* @return
* @throws IOException
*/
    public boolean openChannel(Channel channel) throws IOException {
        return ssh.openChannel(channel);
    }

    /**
 * Implementation of the SessionManager method, this simply calls the SshClient
* method isConnected.
* @return
*/
    public boolean isConnected() {
        return ssh.isConnected();
    }

    /**
* Implementation of the SessionManager method, this simply returns false.
* Overide to change this behaviour
*
* @return
*/
    public boolean requestDisconnect() {
        return disconnectOnClose;
    }

    /**
* Implementation of the SessionManager method, simply calls the SshClient
* method getForwardingClient.
* @return
*/
    public ForwardingClient getForwardingClient() {
        return ssh.getForwardingClient();
    }

    /**
* Implementation of the SessionManager method, simply calls the SshClient
* method sendGlobalRequest.
* @param requestname
* @param wantreply
* @param requestdata
* @return
* @throws IOException
*/
    public byte[] sendGlobalRequest(String requestname, boolean wantreply,
        byte[] requestdata) throws IOException {
        return ssh.sendGlobalRequest(requestname, wantreply, requestdata);
    }

    /**
* Implementation of the SessionManager method, simply calls the SshClient
* method addEventHandler.
* @param eventHandler
*/
    public void addEventHandler(SshEventAdapter eventHandler) {
        ssh.addEventHandler(eventHandler);
    }

    /**
* Implemenation of the SessionManager method, simply calls the SshClient
* method getServerId.
* @return
*/
    public String getServerId() {
        return ssh.getServerId();
    }

    /**
* Implemenation of the SessionManager method, simply calls the SshClient
* method allowChannelOpen.
* @param channelType
* @param cf
* @throws IOException
*/
    public void allowChannelOpen(String channelType, ChannelFactory cf)
        throws IOException {
        ssh.allowChannelOpen(channelType, cf);
    }

    /**
* Gets the profile currently attached to the frame.
* @return
*/
    public SshToolsConnectionProfile getProfile() {
        return profile;
    }
}
