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
package com.sshtools.daemon.session;

import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.connection.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class SessionChannelFactory implements ChannelFactory {
    /**  */
    public final static String SESSION_CHANNEL = "session";
    Class sessionChannelImpl;

    /**
 * Creates a new SessionChannelFactory object.
 *
 * @throws ConfigurationException
 */
    public SessionChannelFactory() throws ConfigurationException {
        sessionChannelImpl = SessionChannelServer.class;

        /*ServerConfiguration server = ConfigurationLoader.getServerConfiguration();
sessionChannelImpl = ConfigurationLoader.getServerConfiguration().getSessionChannelImpl();*/
    }

    /*public List getChannelType() {
 List list = new ArrayList();
 list.add(SessionChannelServer.SESSION_CHANNEL_TYPE);
 return list;
  }*/
    public Channel createChannel(String channelType, byte[] requestData)
        throws InvalidChannelException {
        try {
            if (channelType.equals("session")) {
                return (Channel) sessionChannelImpl.newInstance();
            } else {
                throw new InvalidChannelException(
                    "Only session channels can be opened by this factory");
            }
        } catch (Exception e) {
            throw new InvalidChannelException(
                "Failed to create session channel implemented by " +
                sessionChannelImpl.getName());
        }
    }

    /*public void setSessionChannelImpl(Class sessionChannelImpl)
 throws InvalidChannelException {
 try {
     Channel channel = (Channel) sessionChannelImpl.newInstance();
     if (!(channel instanceof AbstractSessionChannelServer)) {
    throw new InvalidChannelException(
        "Class does not extend AbstractSessionChannelServer");
     }
 } catch (Exception e) {
     throw new InvalidChannelException(
    "Cannot set session channel implementation");
 }
  }*/
}
