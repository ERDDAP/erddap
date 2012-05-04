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
package com.sshtools.daemon.authentication;

import com.sshtools.daemon.platform.*;

import com.sshtools.j2ssh.authentication.*;
import com.sshtools.j2ssh.io.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.10 $
 */
public class PasswordAuthenticationServer extends SshAuthenticationServer {
    private static Log log = LogFactory.getLog(PasswordAuthenticationServer.class);

    /**
 *
 *
 * @return
 */
    public final String getMethodName() {
        return "password";
    }

    /**
 *
 *
 * @param tokens
 */
    public void setAuthenticatedTokens(Map tokens) {
    }

    /**
 *
 *
 * @param authentication
 * @param msg
 *
 * @return
 *
 * @throws IOException
 */
    public int authenticate(AuthenticationProtocolServer authentication,
        SshMsgUserAuthRequest msg) throws IOException {
        NativeAuthenticationProvider authImpl = NativeAuthenticationProvider.getInstance();

        if (authImpl == null) {
            log.error(
                "Cannot perfrom authentication witout native authentication provider");

            return AuthenticationProtocolState.FAILED;
        }

        ByteArrayReader bar = new ByteArrayReader(msg.getRequestData());
        boolean changepwd = ((bar.read() == 0) ? false : true);
        String password = bar.readString();
        String newpassword = null;

        if (changepwd) {
            newpassword = bar.readString();

            try {
                if (!authImpl.changePassword(msg.getUsername(), password,
                            newpassword)) {
                    return AuthenticationProtocolState.FAILED;
                }

                if (authImpl.logonUser(msg.getUsername(), newpassword)) {
                    return AuthenticationProtocolState.COMPLETE;
                } else {
                    return AuthenticationProtocolState.FAILED;
                }
            } catch (PasswordChangeException ex1) {
                return AuthenticationProtocolState.FAILED;
            }
        } else {
            try {
                if (authImpl.logonUser(msg.getUsername(), password)) {
                    log.info(msg.getUsername() +
                        " has passed password authentication");

                    return AuthenticationProtocolState.COMPLETE;
                } else {
                    log.info(msg.getUsername() +
                        " has failed password authentication");

                    return AuthenticationProtocolState.FAILED;
                }
            } catch (PasswordChangeException ex) {
                SshMsgUserAuthPwdChangeReq reply = new SshMsgUserAuthPwdChangeReq(msg.getUsername() +
                        " is required to change password", "");
                authentication.sendMessage(reply);

                return AuthenticationProtocolState.READY;
            }
        }
    }
}
