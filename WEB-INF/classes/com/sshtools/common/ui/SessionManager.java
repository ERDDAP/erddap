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
import com.sshtools.j2ssh.SshEventAdapter;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelFactory;
import com.sshtools.j2ssh.forwarding.ForwardingClient;
import com.sshtools.j2ssh.session.SessionChannelClient;

import java.io.IOException;


/**
 * <p>This interface is used by the Session Provider framework to abstract
 * the SshClient connection away from the session provider. This restricts
 * the session to performing operations that are allowed by the controlling
 * application. For instance, the provider cannot simply diconnect the
 * connection, since the SshClient's disconnect method is not exposed, instead
 * a <code>requestDisconnect</code> method is provided allowing the controlling
 * application to simply ignore a disconnect since it may have other sessions
 * open.
 * </p>
 *
 * <p>
 * Most of the methods of this interface will simply be required to call the
 * identical method on SshClient.
 * </p>
 *
 * @author Lee David Painter
 * @version $Revision: 1.11 $
 */
public interface SessionManager {
    /**
* Opens a session on the managed connection.
*
* @return
*
* @throws IOException
*/
    public SessionChannelClient openSession() throws IOException;

    /**
* The session can call this method to apply any changes to the profile it
* may have made.
*
* @param profile
*/
    public void applyProfileChanges(SshToolsConnectionProfile profile);

    /**
* Opens an SftpClient on the managed connection.
*
* @return
*
* @throws IOException
*/
    public SftpClient openSftpClient() throws IOException;

    /**
* Opens a channel on the managed connection.
*
* @param channel
*
* @return
*
* @throws IOException
*/
    public boolean openChannel(Channel channel) throws IOException;

    /**
* Determine if the managed connection is still connected.
*
* @return
*/
    public boolean isConnected();

    /**
* Called when a session wants to disconnect the connection. The manager
* implementation should ideally not diconnect unless no other sessions
* are open.
*
* @return
*/
    public boolean requestDisconnect();

    /**
* Gets the managed connections port forwarding client.
*
* @return
*/
    public ForwardingClient getForwardingClient();

    /**
* Send a global request
*
* @param requestname
* @param wantreply
* @param requestdata
*
* @return
*
* @throws IOException
*/
    public byte[] sendGlobalRequest(String requestname, boolean wantreply,
        byte[] requestdata) throws IOException;

    /**
* Add an event handler to the managed connection
*
* @param eventHandler
*/
    public void addEventHandler(SshEventAdapter eventHandler);

    /**
* Gets the identification string supplied by the server.
*
* @return
*/
    public String getServerId();

    /**
* Returns the guessed EOL setting of the remote computer
* @return
*/
    public int getRemoteEOL();

    /**
* Adds a channel factory to the managed connection.
*
* @param channelType
* @param cf
*
* @throws IOException
*/
    public void allowChannelOpen(String channelType, ChannelFactory cf)
        throws IOException;

    /**
* Get the current profile attached to the session.
*
* @return
*/
    public SshToolsConnectionProfile getProfile();
}
