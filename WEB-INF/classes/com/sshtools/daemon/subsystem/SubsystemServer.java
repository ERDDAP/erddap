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
package com.sshtools.daemon.subsystem;

import com.sshtools.daemon.session.*;

import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.subsystem.*;
import com.sshtools.j2ssh.transport.*;
import com.sshtools.j2ssh.util.*;

import org.apache.commons.logging.*;

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public abstract class SubsystemServer implements Runnable {
    private static Log log = LogFactory.getLog(SubsystemServer.class);
    private SubsystemMessageStore incoming = new SubsystemMessageStore();
    private SubsystemMessageStore outgoing = new SubsystemMessageStore();
    private SubsystemInputStream in = new SubsystemInputStream(outgoing);
    private SubsystemOutputStream out = new SubsystemOutputStream(incoming);
    private SshThread thread;
    private StartStopState state = new StartStopState(StartStopState.STOPPED);

    /**  */
    protected SessionChannelServer session;

    /**
 * Creates a new SubsystemServer object.
 */
    public SubsystemServer() {
    }

    /**
 *
 *
 * @param session
 */
    public void setSession(SessionChannelServer session) {
        this.session = session;
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public InputStream getInputStream() throws IOException {
        return in;
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    /**
 *
 */
    public void run() {
        state.setValue(StartStopState.STARTED);

        try {
            while (state.getValue() == StartStopState.STARTED) {
                SubsystemMessage msg = incoming.nextMessage();

                if (msg != null) {
                    onMessageReceived(msg);
                }
            }
        } catch (MessageStoreEOFException meof) {
        }

        thread = null;
    }

    /**
 *
 */
    public void start() {
        if (Thread.currentThread() instanceof SshThread) {
            thread = ((SshThread) Thread.currentThread()).cloneThread(this,
                    "SubsystemServer");
            thread.start();
        } else {
            log.error(
                "Subsystem Server must be called from within an SshThread context");
            stop();
        }
    }

    /**
 *
 */
    public void stop() {
        state.setValue(StartStopState.STOPPED);
        incoming.close();
        outgoing.close();
    }

    /**
 *
 *
 * @return
 */
    public StartStopState getState() {
        return state;
    }

    /**
 *
 *
 * @param msg
 */
    protected abstract void onMessageReceived(SubsystemMessage msg);

    /**
 *
 *
 * @param messageId
 * @param implementor
 */
    protected void registerMessage(int messageId, Class implementor) {
        incoming.registerMessage(messageId, implementor);
    }

    /**
 *
 *
 * @param msg
 */
    protected void sendMessage(SubsystemMessage msg) {
        outgoing.addMessage(msg);
    }
}
