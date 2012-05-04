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
package com.sshtools.common.util;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class X11Util {
    // Logger

    /**  */
    protected static Log log = LogFactory.getLog(X11Util.class);
    static byte[] table = {
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x61, 0x62,
        0x63, 0x64, 0x65, 0x66
    };

    /**
*
*
* @param displayNumber
*
* @return
*
* @throws IOException
*/
    public static String getCookie(int displayNumber) throws IOException {
        log.debug("Getting cookie for " + displayNumber + " using xauth");

        Process process = null;
        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;

        //  try {
        byte[] foo = new byte[16];
        ConfigurationLoader.getRND().nextBytes(foo);

        byte[] bar = new byte[32];

        for (int i = 0; i < 16; i++) {
            bar[2 * i] = table[(foo[i] >>> 4) & 0xf];
            bar[(2 * i) + 1] = table[(foo[i]) & 0xf];
        }

        return new String(bar);

        /*    String cmd = "xauth list :" + displayNumber;
log.debug("Executing " + cmd);
process = Runtime.getRuntime().exec(cmd);
IOStreamConnector connect = new IOStreamConnector(
err = process.getErrorStream(), System.out);
BufferedReader reader = new BufferedReader(
new InputStreamReader(in = process.getInputStream()));
out = process.getOutputStream();
String line = null;
String cookie = null;
while( ( line = reader.readLine() ) != null) {
log.debug(line);
StringTokenizer t = new StringTokenizer(line);
try {
    String host = t.nextToken();
    String type = t.nextToken();
    String value = t.nextToken();
    if(cookie == null) {
        cookie = value;
        log.debug("Using cookie " + cookie);
    }
}
catch(Exception e) {
    log.error("Unexpected response from xauth.", e);
}
}
return cookie;
 }
 finally {
IOUtil.closeStream(in);
IOUtil.closeStream(err);
IOUtil.closeStream(out);
 }*/
    }

    /**
*
*
* @param displayNumber
*
* @return
*/
    public static String createCookie(String displayNumber) {
        log.error("Creating fake cookie");

        StringBuffer b = new StringBuffer();

        for (int i = 0; i < 16; i++) {
            int r = (int) (Math.random() * 256);
            String h = Integer.toHexString(r);

            if (h.length() == 1) {
                b.append(0);
            }

            b.append(h);
        }

        log.error("Fake cookie is " + b.toString());

        return b.toString();
    }
}
