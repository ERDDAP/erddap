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
import com.sshtools.j2ssh.transport.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.10 $
 */
public class KBIPasswordAuthenticationServer extends SshAuthenticationServer {
    private static Log log = LogFactory.getLog(KBIPasswordAuthenticationServer.class);

    /**
 *
 *
 * @return
 */
    public final String getMethodName() {
        return "keyboard-interactive";
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
        SshMsgUserAuthRequest msg) throws IOException { //, Map nativeSettings)

        NativeAuthenticationProvider authImpl = NativeAuthenticationProvider.getInstance();

        if (authImpl == null) {
            log.error(
                "Cannot perfrom authentication witout native authentication provider");

            return AuthenticationProtocolState.FAILED;
        }

        authentication.registerMessage(SshMsgUserAuthInfoResponse.SSH_MSG_USERAUTH_INFO_RESPONSE,
            SshMsgUserAuthInfoResponse.class);

        SshMsgUserAuthInfoRequest info = new SshMsgUserAuthInfoRequest("Password authentication",
                "", "");
        info.addPrompt(msg.getUsername() + "'s password", false);
        authentication.sendMessage(info);

        SshMessage response = authentication.readMessage();

        if (response instanceof SshMsgUserAuthInfoResponse) {
            String[] responses = ((SshMsgUserAuthInfoResponse) response).getResponses();

            if (responses.length == 1) {
                String password = responses[0];

                try {
                    if (authImpl.logonUser(msg.getUsername(), password)) { //, nativeSettings)) {
                        log.info(msg.getUsername() +
                            " has passed password authentication");

                        return AuthenticationProtocolState.COMPLETE;
                    } else {
                        log.info(msg.getUsername() +
                            " has failed password authentication");

                        return AuthenticationProtocolState.FAILED;
                    }
                } catch (PasswordChangeException ex) {
                    info = new SshMsgUserAuthInfoRequest("Password change required",
                            "", "");
                    info.addPrompt("New password", false);
                    info.addPrompt("Confirm password", false);
                    authentication.sendMessage(info);
                    response = authentication.readMessage();

                    if (response instanceof SshMsgUserAuthInfoResponse) {
                        responses = ((SshMsgUserAuthInfoResponse) response).getResponses();

                        if (responses.length == 2) {
                            if (responses[0].equals(responses[1])) {
                                if (authImpl.changePassword(msg.getUsername(),
                                            password, responses[0])) {
                                    return AuthenticationProtocolState.COMPLETE;
                                } else {
                                    return AuthenticationProtocolState.FAILED;
                                }
                            } else {
                                return AuthenticationProtocolState.FAILED;
                            }
                        } else {
                            log.error("Client replied with an invalid message " +
                                response.getMessageName());

                            return AuthenticationProtocolState.FAILED;
                        }
                    } else {
                        log.error("Client replied with an invalid message " +
                            response.getMessageName());

                        return AuthenticationProtocolState.FAILED;
                    }
                }
            } else {
                log.error("Client responded with too many values!");

                return AuthenticationProtocolState.FAILED;
            }
        } else {
            log.error("Client replied with an invalid message " +
                response.getMessageName());

            return AuthenticationProtocolState.FAILED;
        }
    }
}
