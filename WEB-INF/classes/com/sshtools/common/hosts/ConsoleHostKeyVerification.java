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
package com.sshtools.common.hosts;

import com.sshtools.j2ssh.transport.InvalidHostFileException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class ConsoleHostKeyVerification extends AbstractHostKeyVerification {
    /**
* Creates a new ConsoleHostKeyVerification object.
*
* @throws InvalidHostFileException
*/
    public ConsoleHostKeyVerification() throws InvalidHostFileException {
        super();
    }

    /**
* Creates a new ConsoleHostKeyVerification object.
*
* @param hostFile
*
* @throws InvalidHostFileException
*/
    public ConsoleHostKeyVerification(String hostFile)
        throws InvalidHostFileException {
        super(hostFile);
    }

    /**
*
*
* @param hostname
*/
    public void onDeniedHost(String hostname) {
        System.out.println("Access to the host " + hostname +
            " is denied from this system");
    }

    /**
*
*
* @param host
* @param fingerprint
* @param actual
*/
    public void onHostKeyMismatch(String host, String fingerprint, String actual) {
        try {
            System.out.println("The host key supplied by " + host + " is: " +
                actual);
            System.out.println("The current allowed key for " + host + " is: " +
                fingerprint);
            getResponse(host, actual);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
*
*
* @param host
* @param fingerprint
*/
    public void onUnknownHost(String host, String fingerprint) {
        try {
            System.out.println("The host " + host +
                " is currently unknown to the system");
            System.out.println("The host key fingerprint is: " + fingerprint);
            getResponse(host, fingerprint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getResponse(String host, String fingerprint)
        throws InvalidHostFileException, IOException {
        String response = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));

        while (!(response.equalsIgnoreCase("YES") ||
                response.equalsIgnoreCase("NO") ||
                (response.equalsIgnoreCase("ALWAYS") && isHostFileWriteable()))) {
            String options = (isHostFileWriteable() ? "Yes|No|Always" : "Yes|No");

            if (!isHostFileWriteable()) {
                System.out.println(
                    "Always option disabled, host file is not writeable");
            }

            System.out.print("Do you want to allow this host key? [" + options +
                "]: ");
            response = reader.readLine();
        }

        if (response.equalsIgnoreCase("YES")) {
            allowHost(host, fingerprint, false);
        }

        if (response.equalsIgnoreCase("ALWAYS") && isHostFileWriteable()) {
            allowHost(host, fingerprint, true);
        }

        // Do nothing on NO
    }
}
