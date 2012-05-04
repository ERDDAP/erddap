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

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;

import java.security.AccessControlException;
import java.security.AccessController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public abstract class AbstractHostKeyVerification extends DefaultHandler
    implements HostKeyVerification {
    private static String defaultHostFile;
    private static Log log = LogFactory.getLog(HostKeyVerification.class);

    static {
        log.info("Determining default host file");

        // Get the sshtools.home system property
        defaultHostFile = ConfigurationLoader.getConfigurationDirectory();

        if (defaultHostFile == null) {
            log.info(
                "No configuration location, persistence of host keys will be disabled.");
        } else {
            // Set the default host file name to our hosts.xml
            defaultHostFile += "hosts.xml";
            log.info("Defaulting host file to " + defaultHostFile);
        }
    }

    private List deniedHosts = new ArrayList();
    private Map allowedHosts = new HashMap();
    private String hostFile;
    private boolean hostFileWriteable;
    private boolean expectEndElement = false;
    private String currentElement = null;

    /**
* Creates a new AbstractHostKeyVerification object.
*
* @throws InvalidHostFileException
*/
    public AbstractHostKeyVerification() throws InvalidHostFileException {
        this(defaultHostFile);
        hostFile = defaultHostFile;
    }

    /**
* Creates a new AbstractHostKeyVerification object.
*
* @param hostFileName
*
* @throws InvalidHostFileException
*/
    public AbstractHostKeyVerification(String hostFileName)
        throws InvalidHostFileException {
        InputStream in = null;

        try {
            //  If no host file is supplied, or there is not enough permission to load
            //  the file, then just create an empty list.
            if (hostFileName != null) {
                if (System.getSecurityManager() != null) {
                    AccessController.checkPermission(new FilePermission(
                            hostFileName, "read"));
                }

                //  Load the hosts file. Do not worry if fle doesnt exist, just disable
                //  save of
                File f = new File(hostFileName);

                if (f.exists()) {
                    in = new FileInputStream(f);
                    hostFile = hostFileName;

                    SAXParserFactory saxFactory = SAXParserFactory.newInstance();
                    SAXParser saxParser = saxFactory.newSAXParser();
                    saxParser.parse(in, this);
                    hostFileWriteable = f.canWrite();
                } else {
                    // Try to create the file
                    if (f.createNewFile()) {
                        FileOutputStream out = new FileOutputStream(f);
                        out.write(toString().getBytes());
                        out.close();
                        hostFileWriteable = true;
                    } else {
                        hostFileWriteable = false;
                    }
                }

                if (!hostFileWriteable) {
                    log.warn("Host file is not writeable.");
                }
            }
        } catch (AccessControlException ace) {
            log.warn(
                "Not enough permission to load a hosts file, so just creating an empty list");
        } catch (IOException ioe) {
            throw new InvalidHostFileException("Could not open or read " +
                hostFileName);
        } catch (SAXException sax) {
            throw new InvalidHostFileException("Failed XML parsing: " +
                sax.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new InvalidHostFileException(
                "Failed to initialize xml parser: " + pce.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
*
*
* @param uri
* @param localName
* @param qname
* @param attrs
*
* @throws SAXException
*/
    public void startElement(String uri, String localName, String qname,
        Attributes attrs) throws SAXException {
        if (currentElement == null) {
            if (qname.equals("HostAuthorizations")) {
                allowedHosts.clear();
                deniedHosts.clear();
                currentElement = qname;
            } else {
                throw new SAXException("Unexpected document element!");
            }
        } else {
            if (!currentElement.equals("HostAuthorizations")) {
                throw new SAXException("Unexpected parent element found!");
            }

            if (qname.equals("AllowHost")) {
                String hostname = attrs.getValue("HostName");
                String fingerprint = attrs.getValue("Fingerprint");

                if ((hostname != null) && (fingerprint != null)) {
                    if (log.isDebugEnabled()) {
                        log.debug("AllowHost element for host '" + hostname +
                            "' with fingerprint '" + fingerprint + "'");
                    }

                    allowedHosts.put(hostname, fingerprint);
                    currentElement = qname;
                } else {
                    throw new SAXException("Requried attribute(s) missing!");
                }
            } else if (qname.equals("DenyHost")) {
                String hostname = attrs.getValue("HostName");

                if (hostname != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("DenyHost element for host " + hostname);
                    }

                    deniedHosts.add(hostname);
                    currentElement = qname;
                } else {
                    throw new SAXException(
                        "Required attribute hostname missing");
                }
            } else {
                log.warn("Unexpected " + qname +
                    " element found in allowed hosts file");
            }
        }
    }

    /**
*
*
* @param uri
* @param localName
* @param qname
*
* @throws SAXException
*/
    public void endElement(String uri, String localName, String qname)
        throws SAXException {
        if (currentElement == null) {
            throw new SAXException("Unexpected end element found!");
        }

        if (currentElement.equals("HostAuthorizations")) {
            currentElement = null;

            return;
        }

        if (currentElement.equals("AllowHost")) {
            currentElement = "HostAuthorizations";

            return;
        }

        if (currentElement.equals("DenyHost")) {
            currentElement = "HostAuthorizations";

            return;
        }
    }

    /**
*
*
* @return
*/
    public boolean isHostFileWriteable() {
        return hostFileWriteable;
    }

    /**
*
*
* @param host
*
* @throws TransportProtocolException
*/
    public abstract void onDeniedHost(String host)
        throws TransportProtocolException;

    /**
*
*
* @param host
* @param allowedHostKey
* @param actualHostKey
*
* @throws TransportProtocolException
*/
    public abstract void onHostKeyMismatch(String host, String allowedHostKey,
        String actualHostKey) throws TransportProtocolException;

    /**
*
*
* @param host
* @param hostKeyFingerprint
*
* @throws TransportProtocolException
*/
    public abstract void onUnknownHost(String host, String hostKeyFingerprint)
        throws TransportProtocolException;

    /**
*
*
* @param host
* @param hostKeyFingerprint
* @param always
*
* @throws InvalidHostFileException
*/
    public void allowHost(String host, String hostKeyFingerprint, boolean always)
        throws InvalidHostFileException {
        if (log.isDebugEnabled()) {
            log.debug("Allowing " + host + " with fingerprint " +
                hostKeyFingerprint);
        }

        // Put the host into the allowed hosts list, overiding any previous
        // entry
        allowedHosts.put(host, hostKeyFingerprint);

        // If we always want to allow then save the host file with the
        // new details
        if (always) {
            saveHostFile();
        }
    }

    /**
*
*
* @return
*/
    public Map allowedHosts() {
        return allowedHosts;
    }

    /**
*
*
* @return
*/
    public java.util.List deniedHosts() {
        return deniedHosts;
    }

    /**
*
*
* @param host
*/
    public void removeAllowedHost(String host) {
        allowedHosts.remove(host);
    }

    /**
*
*
* @param host
*/
    public void removeDeniedHost(String host) {
        for (int i = deniedHosts.size() - 1; i >= 0; i--) {
            String h = (String) deniedHosts.get(i);

            if (h.equals(host)) {
                deniedHosts.remove(i);
            }
        }
    }

    /**
*
*
* @param host
* @param always
*
* @throws InvalidHostFileException
*/
    public void denyHost(String host, boolean always)
        throws InvalidHostFileException {
        if (log.isDebugEnabled()) {
            log.debug(host + " is denied access");
        }

        // Get the denied host from the list
        if (!deniedHosts.contains(host)) {
            deniedHosts.add(host);
        }

        // Save it if need be
        if (always) {
            saveHostFile();
        }
    }

    /**
*
*
* @param host
* @param pk
*
* @return
*
* @throws TransportProtocolException
*/
    public boolean verifyHost(String host, SshPublicKey pk)
        throws TransportProtocolException {
        String fingerprint = pk.getFingerprint();
        log.info("Verifying " + host + " host key");

        if (log.isDebugEnabled()) {
            log.debug("Fingerprint: " + fingerprint);
        }

        // See if the host is denied by looking at the denied hosts list
        if (deniedHosts.contains(host)) {
            onDeniedHost(host);

            return false;
        }

        // Try the allowed hosts by looking at the allowed hosts map
        if (allowedHosts.containsKey(host)) {
            // The host is allowed so check the fingerprint
            String currentFingerprint = (String) allowedHosts.get(host);

            if (currentFingerprint.compareToIgnoreCase(fingerprint) == 0) {
                return true;
            }

            // The host key does not match the recorded so call the abstract
            // method so that the user can decide
            onHostKeyMismatch(host, currentFingerprint, fingerprint);

            // Recheck the after the users input
            return checkFingerprint(host, fingerprint);
        } else {
            // The host is unknown os ask the user
            onUnknownHost(host, fingerprint);

            // Recheck ans return the result
            return checkFingerprint(host, fingerprint);
        }
    }

    private boolean checkFingerprint(String host, String fingerprint) {
        String currentFingerprint = (String) allowedHosts.get(host);

        if (currentFingerprint != null) {
            if (currentFingerprint.compareToIgnoreCase(fingerprint) == 0) {
                return true;
            }
        }

        return false;
    }

    /**
*
*
* @throws InvalidHostFileException
*/
    public void saveHostFile() throws InvalidHostFileException {
        if (!hostFileWriteable) {
            throw new InvalidHostFileException("Host file is not writeable.");
        }

        log.info("Saving " + defaultHostFile);

        try {
            File f = new File(hostFile);
            FileOutputStream out = new FileOutputStream(f);
            out.write(toString().getBytes());
            out.close();
        } catch (IOException e) {
            throw new InvalidHostFileException("Could not write to " +
                hostFile);
        }
    }

    /**
*
*
* @return
*/
    public String toString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<HostAuthorizations>\n";
        xml += "<!-- Host Authorizations file, used by the abstract class HostKeyVerification to verify the servers host key -->";
        xml += "   <!-- Allow the following hosts access if they provide the correct public key -->\n";

        Map.Entry entry;
        Iterator it = allowedHosts.entrySet().iterator();

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            xml += ("   " + "<AllowHost HostName=\"" +
            entry.getKey().toString() + "\" Fingerprint=\"" +
            entry.getValue().toString() + "\"/>\n");
        }

        xml += "   <!-- Deny the following hosts access -->\n";
        it = deniedHosts.iterator();

        while (it.hasNext()) {
            xml += ("   <DenyHost HostName=\"" + it.next().toString() +
            "\"/>\n");
        }

        xml += "</HostAuthorizations>";

        return xml;
    }
}
