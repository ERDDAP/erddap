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
package com.sshtools.daemon.configuration;

import com.sshtools.daemon.session.*;

import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.transport.publickey.*;

import org.apache.commons.logging.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.io.*;

import java.util.*;

import javax.xml.parsers.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class ServerConfiguration extends DefaultHandler {
    private static Log log = LogFactory.getLog(ServerConfiguration.class);
    private Map allowedSubsystems = new HashMap();
    private Map serverHostKeys = new HashMap();
    private List allowedAuthentications = new ArrayList();
    private List requiredAuthentications = new ArrayList();
    private int commandPort = 12222;
    private int port = 22;
    private String listenAddress = "0.0.0.0";
    private int maxConnections = 10;
    private int maxAuthentications = 5;
    private String terminalProvider = "";
    private String authorizationFile = "authorization.xml";
    private String userConfigDirectory = "%D/.ssh2";
    private String authenticationBanner = "";
    private boolean allowTcpForwarding = true;
    private String currentElement = null;
    private Class sessionChannelImpl = SessionChannelServer.class;

    /**
 * Creates a new ServerConfiguration object.
 *
 * @param in
 *
 * @throws SAXException
 * @throws ParserConfigurationException
 * @throws IOException
 */
    public ServerConfiguration(InputStream in)
        throws SAXException, ParserConfigurationException, IOException {
        reload(in);
    }

    /**
 *
 *
 * @param in
 *
 * @throws SAXException
 * @throws ParserConfigurationException
 * @throws IOException
 */
    public void reload(InputStream in)
        throws SAXException, ParserConfigurationException, IOException {
        allowedSubsystems.clear();
        serverHostKeys.clear();
        allowedAuthentications.clear();
        requiredAuthentications.clear();
        commandPort = 12222;
        port = 22;
        listenAddress = "0.0.0.0";
        maxConnections = 10;
        maxAuthentications = 5;
        terminalProvider = "";
        authorizationFile = "authorization.xml";
        userConfigDirectory = "%D/.ssh2";
        authenticationBanner = "";
        allowTcpForwarding = true;
        currentElement = null;

        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        saxParser.parse(in, this);
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
            if (!qname.equals("ServerConfiguration")) {
                throw new SAXException("Unexpected root element " + qname);
            }
        } else {
            if (currentElement.equals("ServerConfiguration")) {
                if (qname.equals("ServerHostKey")) {
                    //String algorithm = attrs.getValue("AlgorithmName");
                    String privateKey = attrs.getValue("PrivateKeyFile");

                    if (privateKey == null) {
                        throw new SAXException(
                            "Required attributes missing from <ServerHostKey> element");
                    }

                    log.debug("ServerHostKey PrivateKeyFile=" + privateKey);

                    File f = new File(privateKey);

                    if (!f.exists()) {
                        privateKey = ConfigurationLoader.getConfigurationDirectory() +
                            privateKey;
                        f = new File(privateKey);
                    }

                    try {
                        if (f.exists()) {
                            SshPrivateKeyFile pkf = SshPrivateKeyFile.parse(f);
                            SshPrivateKey key = pkf.toPrivateKey(null);
                            serverHostKeys.put(key.getAlgorithmName(), key);
                        } else {
                            log.warn("Private key file '" + privateKey +
                                "' could not be found");
                        }
                    } catch (InvalidSshKeyException ex) {
                        log.warn("Failed to load private key '" + privateKey, ex);
                    } catch (IOException ioe) {
                        log.warn("Failed to load private key '" + privateKey,
                            ioe);
                    }
                } else if (qname.equals("Subsystem")) {
                    String type = attrs.getValue("Type");
                    String name = attrs.getValue("Name");
                    String provider = attrs.getValue("Provider");

                    if ((type == null) || (name == null) || (provider == null)) {
                        throw new SAXException(
                            "Required attributes missing from <Subsystem> element");
                    }

                    log.debug("Subsystem Type=" + type + " Name=" + name +
                        " Provider=" + provider);
                    allowedSubsystems.put(name,
                        new AllowedSubsystem(type, name, provider));
                } else if (!qname.equals("AuthenticationBanner") &&
                        !qname.equals("MaxConnections") &&
                        !qname.equals("MaxAuthentications") &&
                        !qname.equals("ListenAddress") &&
                        !qname.equals("Port") && !qname.equals("CommandPort") &&
                        !qname.equals("TerminalProvider") &&
                        !qname.equals("AllowedAuthentication") &&
                        !qname.equals("RequiredAuthentication") &&
                        !qname.equals("AuthorizationFile") &&
                        !qname.equals("UserConfigDirectory") &&
                        !qname.equals("AllowTcpForwarding")) {
                    throw new SAXException("Unexpected <" + qname +
                        "> element after SshAPIConfiguration");
                }
            }
        }

        currentElement = qname;
    }

    /**
 *
 *
 * @param ch
 * @param start
 * @param length
 *
 * @throws SAXException
 */
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        String value = new String(ch, start, length);

        if (currentElement != null) {
            if (currentElement.equals("AuthenticationBanner")) {
                authenticationBanner = value;
                log.debug("AuthenticationBanner=" + authenticationBanner);
            } else if (currentElement.equals("MaxConnections")) {
                maxConnections = Integer.parseInt(value);
                log.debug("MaxConnections=" + value);
            } else if (currentElement.equals("ListenAddress")) {
                listenAddress = value;
                log.debug("ListenAddress=" + listenAddress);
            } else if (currentElement.equals("Port")) {
                port = Integer.parseInt(value);
                log.debug("Port=" + value);
            } else if (currentElement.equals("CommandPort")) {
                commandPort = Integer.parseInt(value);
                log.debug("CommandPort=" + value);
            } else if (currentElement.equals("TerminalProvider")) {
                terminalProvider = value;
                log.debug("TerminalProvider=" + terminalProvider);
            } else if (currentElement.equals("AllowedAuthentication")) {
                if (!allowedAuthentications.contains(value)) {
                    allowedAuthentications.add(value);
                    log.debug("AllowedAuthentication=" + value);
                }
            } else if (currentElement.equals("RequiredAuthentication")) {
                if (!requiredAuthentications.contains(value)) {
                    requiredAuthentications.add(value);
                    log.debug("RequiredAuthentication=" + value);
                }
            } else if (currentElement.equals("AuthorizationFile")) {
                authorizationFile = value;
                log.debug("AuthorizationFile=" + authorizationFile);
            } else if (currentElement.equals("UserConfigDirectory")) {
                userConfigDirectory = value;
                log.debug("UserConfigDirectory=" + userConfigDirectory);
            } else if (currentElement.equals("SessionChannelImpl")) {
                try {
                    sessionChannelImpl = ConfigurationLoader.getExtensionClass(value);
                } catch (Exception e) {
                    log.error("Failed to load SessionChannelImpl " + value, e);
                }
            } else if (currentElement.equals("MaxAuthentications")) {
                maxAuthentications = Integer.parseInt(value);
                log.debug("MaxAuthentications=" + value);
            } else if (currentElement.equals("AllowTcpForwarding")) {
                allowTcpForwarding = Boolean.valueOf(value).booleanValue();
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
        if (currentElement != null) {
            if (!currentElement.equals(qname)) {
                throw new SAXException("Unexpected end element found <" +
                    qname + ">");
            } else if (currentElement.equals("ServerConfiguration")) {
                currentElement = null;
            } else if (currentElement.equals("AuthenticationBanner") ||
                    currentElement.equals("ServerHostKey") ||
                    currentElement.equals("Subsystem") ||
                    currentElement.equals("MaxConnections") ||
                    currentElement.equals("MaxAuthentications") ||
                    currentElement.equals("ListenAddress") ||
                    currentElement.equals("Port") ||
                    currentElement.equals("CommandPort") ||
                    currentElement.equals("TerminalProvider") ||
                    currentElement.equals("AllowedAuthentication") ||
                    currentElement.equals("RequiredAuthentication") ||
                    currentElement.equals("AuthorizationFile") ||
                    currentElement.equals("UserConfigDirectory") ||
                    currentElement.equals("AllowTcpForwarding")) {
                currentElement = "ServerConfiguration";
            }
        } else {
            throw new SAXException("Unexpected end element <" + qname +
                "> found");
        }
    }

    /**
 *
 *
 * @return
 */
    public List getRequiredAuthentications() {
        return requiredAuthentications;
    }

    /**
 *
 *
 * @return
 */
    public List getAllowedAuthentications() {
        return allowedAuthentications;
    }

    /**
 *
 *
 * @return
 */
    public boolean getAllowTcpForwarding() {
        return allowTcpForwarding;
    }

    /**
 *
 *
 * @return
 */
    public String getAuthenticationBanner() {
        return authenticationBanner;
    }

    /**
 *
 *
 * @return
 */
    public int getCommandPort() {
        return commandPort;
    }

    /**
 *
 *
 * @return
 */
    public String getUserConfigDirectory() {
        return userConfigDirectory;
    }

    /**
 *
 *
 * @return
 */
    public String getAuthorizationFile() {
        return authorizationFile;
    }

    /**
 *
 *
 * @return
 */
    public String getListenAddress() {
        return listenAddress;
    }

    /**
 *
 *
 * @return
 */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
 *
 *
 * @return
 */
    public int getMaxAuthentications() {
        return maxAuthentications;
    }

    /**
 *
 *
 * @return
 */
    public int getPort() {
        return port;
    }

    /*public Class getSessionChannelImpl() {
 return sessionChannelImpl;
  }*/
    public Map getServerHostKeys() {
        return serverHostKeys;
    }

    /**
 *
 *
 * @return
 */
    public Map getSubsystems() {
        return allowedSubsystems;
    }

    /**
 *
 *
 * @return
 */
    public String getTerminalProvider() {
        return terminalProvider;
    }

    /**
 *
 *
 * @return
 */
    public String toString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xml += "<!-- Server configuration file - If filenames are not absolute they are assummed to be in the same directory as this configuration file. -->\n";
        xml += "<ServerConfiguration>\n";
        xml += "   <!-- The available host keys for server authentication -->\n";

        Map.Entry entry;
        Iterator it = serverHostKeys.entrySet().iterator();

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            xml += ("   <ServerHostKey PrivateKeyFile=\"" + entry.getValue() +
            "\"/>\n");
        }

        xml += "   <!-- Add any number of subsystem elements here -->\n";

        AllowedSubsystem subsystem;
        it = allowedSubsystems.entrySet().iterator();

        while (it.hasNext()) {
            subsystem = (AllowedSubsystem) ((Map.Entry) it.next()).getValue();
            xml += ("   <Subsystem Name=\"" + subsystem.getName() +
            "\" Type=\"" + subsystem.getType() + "\" Provider=\"" +
            subsystem.getProvider() + "\"/>\n");
        }

        xml += "   <!-- Display the following authentication banner before authentication -->\n";
        xml += ("   <AuthenticationBanner>" + authenticationBanner +
        "</AuthenticationBanner>\n");
        xml += "   <!-- The maximum number of connected sessions available -->\n";
        xml += ("   <MaxConnections>" + String.valueOf(maxConnections) +
        "</MaxConnections>\n");
        xml += "   <!-- The maximum number of authentication attemtps for each connection -->\n";
        xml += ("   <MaxAuthentications>" + String.valueOf(maxAuthentications) +
        "</MaxAuthentications>\n");
        xml += "   <!-- Bind to the following address to listen for connections -->\n";
        xml += ("   <ListenAddress>" + listenAddress + "</ListenAddress>\n");
        xml += "   <!-- The port to listen to -->\n";
        xml += ("   <Port>" + String.valueOf(port) + "</Port>\n");
        xml += "   <!-- Listen on the following port (on localhost) for server commands such as stop -->\n";
        xml += ("   <CommandPort>" + String.valueOf(commandPort) +
        "</CommandPort>\n");
        xml += "   <!-- Specify the executable that provides the default shell -->\n";
        xml += ("   <TerminalProvider>" + terminalProvider +
        "</TerminalProvider>\n");
        xml += "   <!-- Specify any number of allowed authentications -->\n";
        it = allowedAuthentications.iterator();

        while (it.hasNext()) {
            xml += ("   <AllowedAuthentication>" + it.next().toString() +
            "</AllowedAuthentication>\n");
        }

        xml += "   <!-- Specify any number of required authentications -->\n";
        it = requiredAuthentications.iterator();

        while (it.hasNext()) {
            xml += ("   <RequiredAuthentication>" + it.next().toString() +
            "</RequiredAuthentication>\n");
        }

        xml += "   <!-- The users authorizations file -->\n";
        xml += ("   <AuthorizationFile>" + authorizationFile +
        "</AuthorizationFile>\n");
        xml += "   <!-- The users configuration directory where files such as AuthorizationFile are found. For users home directory specify %D For users name specify %U  -->\n";
        xml += ("   <UserConfigDirectory>" + userConfigDirectory +
        "</UserConfigDirectory>\n");
        xml += ("<AllowTcpForwarding>" + String.valueOf(allowTcpForwarding) +
        "</AllowTcpForwarding>\n");
        xml += "</ServerConfiguration>\n";

        return xml;
    }
}
