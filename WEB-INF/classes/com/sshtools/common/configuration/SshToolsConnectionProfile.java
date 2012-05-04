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
package com.sshtools.common.configuration;

import com.sshtools.common.util.PropertyUtil;

import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClientFactory;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.forwarding.ForwardingConfiguration;
import com.sshtools.j2ssh.io.IOUtil;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.awt.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.22 $
 */
public class SshToolsConnectionProfile extends SshConnectionProperties {
    private static Log log = LogFactory.getLog(SshToolsConnectionProfile.class);

    /**  */
    public static final int DO_NOTHING = 1;

    /**  */
    public static final int START_SHELL = 2;

    /**  */
    public static final int EXECUTE_COMMANDS = 3;
    private Map applicationProperties = new HashMap();
    private Map sftpFavorites = new HashMap();
    private Map authMethods = new HashMap();
    private boolean requestPseudoTerminal = true;
    private boolean disconnectOnSessionClose = true;
    private int onceAuthenticated = START_SHELL;
    private String executeCommands = "";
    private boolean allowAgentForwarding = false;

    // SAX Processing variables
    private String currentElement = null;
    private String currentAuthentication = null;
    private Properties currentProperties = null;
    private String connectionFile;

    /**
* Creates a new SshToolsConnectionProfile object.
*/
    public SshToolsConnectionProfile() {
    }

    /**
*
*
* @return
*/
    public Map getAuthenticationMethods() {
        return authMethods;
    }

    /**
*
*
* @return
*/
    public boolean requiresPseudoTerminal() {
        return requestPseudoTerminal;
    }

    /**
*
*
* @return
*/
    public boolean disconnectOnSessionClose() {
        return disconnectOnSessionClose;
    }

    /**
*
*
* @param requiresPseudoTerminal
*/
    public void setRequiresPseudoTerminal(boolean requiresPseudoTerminal) {
        this.requestPseudoTerminal = requiresPseudoTerminal;
    }

    /**
*
*
* @param disconnectOnSessionClose
*/
    public void setDisconnectOnSessionClose(boolean disconnectOnSessionClose) {
        this.disconnectOnSessionClose = disconnectOnSessionClose;
    }

    /**
*
*/
    public void clearAuthenticationCache() {
        SshAuthenticationClient auth;
        Properties properties;

        for (Iterator it = authMethods.values().iterator(); it.hasNext();) {
            auth = (SshAuthenticationClient) it.next();
            properties = auth.getPersistableProperties();
            auth.reset();
            auth.setPersistableProperties(properties);
        }
    }

    /**
*
*
* @param onceAuthenticated
*/
    public void setOnceAuthenticatedCommand(int onceAuthenticated) {
        this.onceAuthenticated = onceAuthenticated;
    }

    /**
*
*
* @return
*/
    public int getOnceAuthenticatedCommand() {
        return onceAuthenticated;
    }

    /**
*
*
* @param executeCommands
*/
    public void setCommandsToExecute(String executeCommands) {
        this.executeCommands = executeCommands;
    }

    /**
*
*
* @return
*/
    public String getCommandsToExecute() {
        return executeCommands;
    }

    /**
*
*
* @param name
* @param defaultValue
*
* @return
*/
    public String getApplicationProperty(String name, String defaultValue) {
        String value = (String) applicationProperties.get(name);

        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
*
*
* @param name
* @param defaultValue
*
* @return
*/
    public Map getSftpFavorites() {
        return sftpFavorites;
    }

    /**
*
*
* @param name
* @param defaultValue
*
* @return
*/
    public void setSftpFavorite(String name, String value) {
        sftpFavorites.put(name, value);
    }

    /**
*
*
* @param name
* @param defaultValue
*
* @return
*/
    public int getApplicationPropertyInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(getApplicationProperty(name,
                    String.valueOf(defaultValue)));
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
*
*
* @param name
* @param defaultValue
*
* @return
*/
    public boolean getApplicationPropertyBoolean(String name,
        boolean defaultValue) {
        try {
            return new Boolean(getApplicationProperty(name,
                    String.valueOf(defaultValue))).booleanValue();
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
*
*
* @param name
* @param defaultColor
*
* @return
*/
    public Color getApplicationPropertyColor(String name, Color defaultColor) {
        return PropertyUtil.stringToColor(getApplicationProperty(name,
                PropertyUtil.colorToString(defaultColor)));
    }

    /**
*
*
* @param name
* @param value
*/
    public void setApplicationProperty(String name, String value) {
        applicationProperties.put(name, value);
    }

    /**
*
*
* @param name
* @param value
*/
    public void setApplicationProperty(String name, int value) {
        applicationProperties.put(name, String.valueOf(value));
    }

    /**
*
*
* @param name
* @param value
*/
    public void setApplicationProperty(String name, boolean value) {
        applicationProperties.put(name, String.valueOf(value));
    }

    /**
*
*
* @param name
* @param value
*/
    public void setApplicationProperty(String name, Color value) {
        applicationProperties.put(name, PropertyUtil.colorToString(value));
    }

    /**
*
*
* @param method
*/
    public void addAuthenticationMethod(SshAuthenticationClient method) {
        if (method != null) {
            if (!authMethods.containsKey(method.getMethodName())) {
                authMethods.put(method.getMethodName(), method);
            }
        }
    }

    /**
*
*
* @param config
*/
    public void addLocalForwarding(ForwardingConfiguration config) {
        if (config != null) {
            localForwardings.put(config.getName(), config);
        }
    }

    /**
*
*
* @param config
*/
    public void addRemoteForwarding(ForwardingConfiguration config) {
        if (config != null) {
            remoteForwardings.put(config.getName(), config);
        }
    }

    /**
*
*
* @return
*/
    public boolean getAllowAgentForwarding() {
        return allowAgentForwarding;
    }

    /**
*
*
* @param allowAgentForwarding
*/
    public void setAllowAgentForwarding(boolean allowAgentForwarding) {
        this.allowAgentForwarding = allowAgentForwarding;
    }

    /**
*
*
* @param name
*/
    public void removeLocalForwarding(String name) {
        localForwardings.remove(name);
    }

    /**
*
*
* @param name
*/
    public void removeRemoteForwarding(String name) {
        remoteForwardings.remove(name);
    }

    /**
*
*
* @param file
*
* @throws InvalidProfileFileException
*/
    public void open(String file) throws InvalidProfileFileException {
        connectionFile = file;
        open(new File(file));
    }

    /**
*
*
* @param file
*
* @throws InvalidProfileFileException
*/
    public void open(File file) throws InvalidProfileFileException {
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            open(in);
        } catch (FileNotFoundException fnfe) {
            throw new InvalidProfileFileException(file + " was not found!");
        } finally {
            IOUtil.closeStream(in);
        }
    }

    /**
*
*
* @param in
*
* @throws InvalidProfileFileException
*/
    public void open(InputStream in) throws InvalidProfileFileException {
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxFactory.newSAXParser();
            XMLHandler handler = new XMLHandler();
            saxParser.parse(in, handler);
            handler = null;

            //            in.close();
        } catch (IOException ioe) {
            throw new InvalidProfileFileException("IO error. " +
                ioe.getMessage());
        } catch (SAXException sax) {
            throw new InvalidProfileFileException("SAX Error: " +
                sax.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new InvalidProfileFileException("SAX Parser Error: " +
                pce.getMessage());
        } finally {
        }
    }

    /**
*
*
* @param method
*/
    public void removeAuthenticaitonMethod(String method) {
        authMethods.remove(method);
    }

    public void removeAuthenticationMethods() {
        authMethods.clear();
    }

    /**
*
*
* @param file
*
* @throws InvalidProfileFileException
*/
    public void save(String file) throws InvalidProfileFileException {
        try {
            File f = new File(file);
            FileOutputStream out = new FileOutputStream(f);
            out.write(toString().getBytes());
            out.close();
        } catch (FileNotFoundException fnfe) {
            throw new InvalidProfileFileException(file + " was not found!");
        } catch (IOException ioe) {
            throw new InvalidProfileFileException("io error on " + file);
        } finally {
        }
    }

    public void save() throws InvalidProfileFileException {
        save(connectionFile);
    }

    /**
*
*
* @return
*/
    public String toString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xml += ("<SshToolsConnectionProfile Hostname=\"" + host + "\" Port=\"" +
        String.valueOf(port) + "\" Username=\"" + username + "\"" +
        " Provider=\"" + getTransportProviderString() + "\">");
        xml += ("   <PreferedCipher Client2Server=\"" + prefEncryption +
        "\" Server2Client=\"" + prefDecryption + "\"/>\n");
        xml += ("   <PreferedMac Client2Server=\"" + prefRecvMac +
        "\" Server2Client=\"" + prefSendMac + "\"/>\n");
        xml += ("   <PreferedCompression Client2Server=\"" + prefRecvComp +
        "\" Server2Client=\"" + prefSendComp + "\"/>\n");
        xml += ("   <PreferedPublicKey Name=\"" + prefPK + "\"/>\n");
        xml += ("   <PreferedKeyExchange Name=\"" + prefKex + "\"/>\n");
        xml += ("   <OnceAuthenticated value=\"" +
        String.valueOf(onceAuthenticated) + "\"/>\n");

        if (onceAuthenticated == EXECUTE_COMMANDS) {
            xml += ("    <ExecuteCommands>" + executeCommands +
            "</ExecuteCommands>\n");
        }

        Iterator it = authMethods.entrySet().iterator();
        Map.Entry entry;
        Properties properties;

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            xml += ("   <AuthenticationMethod Name=\"" + entry.getKey() +
            "\">\n");

            SshAuthenticationClient auth = (SshAuthenticationClient) entry.getValue();
            properties = auth.getPersistableProperties();

            Iterator it2 = properties.entrySet().iterator();

            while (it2.hasNext()) {
                entry = (Map.Entry) it2.next();
                xml += ("      <AuthenticationProperty Name=\"" +
                entry.getKey() + "\" Value=\"" + entry.getValue() + "\"/>\n");
            }

            xml += "   </AuthenticationMethod>\n";
        }

        it = applicationProperties.entrySet().iterator();

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            xml += ("   <ApplicationProperty Name=\"" + entry.getKey() +
            "\" Value=\"" + entry.getValue() + "\"/>\n");
        }

        // Write the SFTP Favorite entries to file
        it = sftpFavorites.entrySet().iterator();

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            xml += ("   <SftpFavorite Name=\"" + entry.getKey() +
            "\" Value=\"" + entry.getValue() + "\"/>\n");
        }

        it = localForwardings.values().iterator();
        xml += ("   <ForwardingAutoStart value=\"" +
        String.valueOf(forwardingAutoStart) + "\"/>\n");

        while (it.hasNext()) {
            ForwardingConfiguration config = (ForwardingConfiguration) it.next();
            xml += ("   <LocalPortForwarding Name=\"" + config.getName() +
            "\" AddressToBind=\"" + config.getAddressToBind() +
            "\" PortToBind=\"" + String.valueOf(config.getPortToBind()) +
            "\" AddressToConnect=\"" + config.getHostToConnect() +
            "\" PortToConnect=\"" + String.valueOf(config.getPortToConnect()) +
            "\"/>\n");
        }

        it = remoteForwardings.values().iterator();

        while (it.hasNext()) {
            ForwardingConfiguration config = (ForwardingConfiguration) it.next();
            xml += ("   <RemotePortForwarding Name=\"" + config.getName() +
            "\" AddressToBind=\"" + config.getAddressToBind() +
            "\" PortToBind=\"" + String.valueOf(config.getPortToBind()) +
            "\" AddressToConnect=\"" + config.getHostToConnect() +
            "\" PortToConnect=\"" + String.valueOf(config.getPortToConnect()) +
            "\"/>\n");
        }

        xml += "</SshToolsConnectionProfile>";

        return xml;
    }

    private class XMLHandler extends DefaultHandler {
        boolean commandsToExecute = false;

        public void startElement(String uri, String localName, String qname,
            Attributes attrs) throws SAXException {
            if (currentElement == null) {
                if (!qname.equals("SshToolsConnectionProfile")) {
                    throw new SAXException("Unexpected root element " + qname);
                }

                host = attrs.getValue("Hostname");
                username = attrs.getValue("Username");

                String p = attrs.getValue("Port");

                if (p == null) {
                    port = 22;
                } else {
                    port = Integer.parseInt(p);
                }

                setTransportProviderString(attrs.getValue("Provider"));

                if ((host == null) || (username == null)) {
                    throw new SAXException(
                        "Required attribute for element <SshToolsConnectionProfile> missing");
                }
            } else {
                String c2s;
                String s2c;

                if (currentElement.equals("SshToolsConnectionProfile")) {
                    if (qname.equals("PreferedCipher")) {
                        c2s = attrs.getValue("Client2Server");
                        s2c = attrs.getValue("Server2Client");

                        if ((c2s == null) || (s2c == null)) {
                            throw new SAXException(
                                "Required attribute missing for <PreferedCipher> element");
                        }

                        prefEncryption = c2s;
                        prefDecryption = s2c;
                    } else if (qname.equals("OnceAuthenticated")) {
                        if (attrs.getValue("value") != null) {
                            try {
                                onceAuthenticated = Integer.parseInt(attrs.getValue(
                                            "value"));
                            } catch (NumberFormatException ex) {
                                onceAuthenticated = START_SHELL;
                            }
                        }
                    } else if (qname.equals("ExecuteCommands")) {
                        commandsToExecute = true;
                        executeCommands = "";
                    } else if (qname.equals("PreferedCompression")) {
                        c2s = attrs.getValue("Client2Server");
                        s2c = attrs.getValue("Server2Client");

                        if ((c2s == null) || (s2c == null)) {
                            throw new SAXException(
                                "Required attribute missing for <PreferedCompression> element");
                        }

                        prefRecvComp = c2s;
                        prefSendComp = s2c;
                    } else if (qname.equals("PreferedMac")) {
                        c2s = attrs.getValue("Client2Server");
                        s2c = attrs.getValue("Server2Client");

                        if ((c2s == null) || (s2c == null)) {
                            throw new SAXException(
                                "Required attribute missing for <PreferedMac> element");
                        }

                        prefRecvMac = c2s;
                        prefSendMac = s2c;
                    } else if (qname.equals("PreferedPublicKey")) {
                        String name = attrs.getValue("Name");

                        if (name == null) {
                            throw new SAXException(
                                "Required attribute missing for <PreferedPublickey> element");
                        }

                        prefPK = name;
                    } else if (qname.equals("PreferedKeyExchange")) {
                        String name = attrs.getValue("Name");

                        if (name == null) {
                            throw new SAXException(
                                "Required attribute missing for <PreferedKeyExchange> element");
                        }

                        prefPK = name;
                    } else if (qname.equals("ApplicationProperty")) {
                        String name = attrs.getValue("Name");
                        String value = attrs.getValue("Value");

                        if ((name == null) || (value == null)) {
                            throw new SAXException(
                                "Required attributes missing for <ApplicationProperty> element");
                        }

                        applicationProperties.put(name, value);
                    } else if (qname.equals("SftpFavorite")) {
                        String name = attrs.getValue("Name");
                        String value = attrs.getValue("Value");

                        if ((name == null) || (value == null)) {
                            throw new SAXException(
                                "Required attributes missing for <SftpFavorite> element");
                        }

                        sftpFavorites.put(name, value);
                    } else if (qname.equals("AuthenticationMethod")) {
                        currentAuthentication = attrs.getValue("Name");
                        currentProperties = new Properties();

                        if (currentAuthentication == null) {
                            throw new SAXException(
                                "Required attribute missing for <AuthenticationMethod> element");
                        }
                    } else if (qname.equals("LocalPortForwarding") ||
                            qname.equals("RemotePortForwarding")) {
                        String name = attrs.getValue("Name");
                        String addressToBind = attrs.getValue("AddressToBind");
                        String portToBind = attrs.getValue("PortToBind");
                        String addressToConnect = attrs.getValue(
                                "AddressToConnect");
                        String portToConnect = attrs.getValue("PortToConnect");

                        if ((name == null) || (addressToBind == null) ||
                                (portToBind == null) ||
                                (addressToConnect == null) ||
                                (portToConnect == null)) {
                            throw new SAXException(
                                "Required attribute missing for <" + qname +
                                "> element");
                        }

                        ForwardingConfiguration config = new ForwardingConfiguration(name,
                                addressToBind, Integer.parseInt(portToBind),
                                addressToConnect,
                                Integer.parseInt(portToConnect));

                        if (qname.equals("LocalPortForwarding")) {
                            localForwardings.put(name, config);
                        } else {
                            remoteForwardings.put(name, config);
                        }
                    } else if (qname.equals("ForwardingAutoStart")) {
                        try {
                            forwardingAutoStart = Boolean.valueOf(attrs.getValue(
                                        "value")).booleanValue();
                        } catch (Throwable ex1) {
                        }
                    } else {
                        throw new SAXException("Unexpected element <" + qname +
                            "> after SshToolsConnectionProfile");
                    }
                } else if (currentElement.equals("AuthenticationMethod")) {
                    if (qname.equals("AuthenticationProperty")) {
                        String name = attrs.getValue("Name");
                        String value = attrs.getValue("Value");

                        if ((name == null) || (value == null)) {
                            throw new SAXException(
                                "Required attribute missing for <AuthenticationProperty> element");
                        }

                        currentProperties.setProperty(name, value);
                    } else {
                        throw new SAXException("Unexpected element <" + qname +
                            "> found after AuthenticationMethod");
                    }
                }
            }

            currentElement = qname;
        }

        public void characters(char[] ch, int pos, int len) {
            executeCommands += new String(ch, pos, len);
        }

        public void endElement(String uri, String localName, String qname)
            throws SAXException {
            if (currentElement != null) {
                if (!currentElement.equals(qname)) {
                    throw new SAXException("Unexpected end element found " +
                        qname);
                } else if (qname.equals("SshToolsConnectionProfile")) {
                    currentElement = null;
                } else if (qname.startsWith("Prefered")) {
                    currentElement = "SshToolsConnectionProfile";
                } else if (qname.equals("OnceAuthenticated")) {
                    currentElement = "SshToolsConnectionProfile";
                } else if (qname.equals("ExecuteCommands")) {
                    currentElement = "SshToolsConnectionProfile";
                } else if (qname.equals("ApplicationProperty")) {
                    currentElement = "SshToolsConnectionProfile";
                } else if (qname.equals("SftpFavorite")) {
                    currentElement = "SshToolsConnectionProfile";
                } else if (qname.equals("AuthenticationProperty")) {
                    currentElement = "AuthenticationMethod";
                } else if (qname.equals("LocalPortForwarding") ||
                        qname.equals("RemotePortForwarding") ||
                        qname.equals("ForwardingAutoStart")) {
                    currentElement = "SshToolsConnectionProfile";
                } else if (qname.equals("AuthenticationMethod")) {
                    currentElement = "SshToolsConnectionProfile";

                    try {
                        SshAuthenticationClient auth = SshAuthenticationClientFactory.newInstance(currentAuthentication);
                        auth.setPersistableProperties(currentProperties);
                        authMethods.put(currentAuthentication, auth);
                    } catch (AlgorithmNotSupportedException anse) {
                        log.warn(
                            "AuthenticationMethod element ignored because '" +
                            currentAuthentication +
                            "' authentication is not supported");
                    } finally {
                        currentAuthentication = null;
                    }
                } else {
                    throw new SAXException("Unexpected end element <" + qname +
                        "> found");
                }
            }
        }
    }
}
