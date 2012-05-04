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

import com.sshtools.daemon.vfs.*;

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
 * @version $Revision: 1.13 $
 */
public class PlatformConfiguration extends DefaultHandler {
    private static Log log = LogFactory.getLog(PlatformConfiguration.class);
    private static final String PLATFORM_ELEMENT = "PlatformConfiguration";
    private static final String NATIVE_PROCESS_ELEMENT = "NativeProcessProvider";
    private static final String NATIVE_AUTH_ELEMENT = "NativeAuthenticationProvider";
    private static final String NFS_ELEMENT = "NativeFileSystemProvider";
    private static final String NATIVE_SETTING_ELEMENT = "NativeSetting";
    private static final String VFSMOUNT_ELEMENT = "VFSMount";
    private static final String VFSROOT_ELEMENT = "VFSRoot";
    private static final String VFSPERMISSION_ELEMENT = "VFSPermission";
    private static final String PATH_ATTRIBUTE = "path";
    private static final String MOUNT_ATTRIBUTE = "mount";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String PERMISSIONS_ATTRIBUTE = "permissions";
    private String currentElement = null;
    private Map nativeSettings = new HashMap();
    private String nativeProcessProvider = null;
    private String nativeAuthenticationProvider = null;
    private String nativeFileSystemProvider = null;
    private Map vfsMounts = new HashMap();
    private VFSMount vfsRoot = null;

    /**
 * Creates a new PlatformConfiguration object.
 *
 * @param in
 *
 * @throws SAXException
 * @throws ParserConfigurationException
 * @throws IOException
 */
    protected PlatformConfiguration(InputStream in)
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
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        saxParser.parse(in, new PlatformConfigurationSAXHandler());
    }

    /**
 *
 *
 * @return
 */
    public Map getVFSMounts() {
        return vfsMounts;
    }

    /**
 *
 *
 * @return
 */
    public String getNativeAuthenticationProvider() {
        return nativeAuthenticationProvider;
    }

    /**
 *
 *
 * @return
 */
    public String getNativeFileSystemProvider() {
        return nativeFileSystemProvider;
    }

    /**
 *
 *
 * @return
 */
    public String getNativeProcessProvider() {
        return nativeProcessProvider;
    }

    /**
 *
 *
 * @param name
 *
 * @return
 */
    public String getSetting(String name) {
        return (String) nativeSettings.get(name);
    }

    /**
 *
 *
 * @param name
 * @param defaultValue
 *
 * @return
 */
    public String getSetting(String name, String defaultValue) {
        if (nativeSettings.containsKey(name)) {
            return (String) nativeSettings.get(name);
        } else {
            return defaultValue;
        }
    }

    /**
 *
 *
 * @param name
 *
 * @return
 */
    public boolean containsSetting(String name) {
        return nativeSettings.containsKey(name);
    }

    /**
 *
 *
 * @return
 */
    public VFSMount getVFSRoot() {
        return vfsRoot;
    }

    /**
 *
 *
 * @return
 */
    public String toString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xml += ("<!-- Platform Configuration file, Determines the behaviour of platform specific services -->\n<" +
        PLATFORM_ELEMENT + ">\n");
        xml += "   <!-- The process provider for executing and redirecting a process -->\n";
        xml += ("   <" + NATIVE_PROCESS_ELEMENT + ">" + nativeProcessProvider +
        "</" + NATIVE_PROCESS_ELEMENT + ">\n");
        xml += "   <!-- The authentication provider for authenticating users and obtaining user information -->\n";
        xml += ("   <" + NATIVE_AUTH_ELEMENT + ">" +
        nativeAuthenticationProvider + "</" + NATIVE_AUTH_ELEMENT + ">\n");
        xml += "   <!-- Native settings which may be used by the process or authentication provider -->\n";

        Map.Entry entry;
        Iterator it = nativeSettings.entrySet().iterator();

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            xml += ("   " + "<" + NATIVE_SETTING_ELEMENT + " " +
            NAME_ATTRIBUTE + "=\"" + entry.getKey().toString() + "\" " +
            VALUE_ATTRIBUTE + "=\"" + entry.getValue().toString() + "\"/>\n");
        }

        if (vfsRoot != null) {
            xml += ("   " + "<" + VFSROOT_ELEMENT + " path=\"" + vfsRoot +
            "\"/>\n");
        }

        it = vfsMounts.entrySet().iterator();

        String path;
        String mount;

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            path = (String) entry.getValue();
            mount = (String) entry.getKey();
            xml += ("   " + "<" + VFSMOUNT_ELEMENT + " " +
            (mount.equals(path) ? ""
                                : (MOUNT_ATTRIBUTE + "=\"" +
            entry.getKey().toString() + "\" ")) + PATH_ATTRIBUTE + "=\"" +
            entry.getValue().toString() + "\"/>\n");
        }

        xml += ("</" + PLATFORM_ELEMENT + ">");

        return xml;
    }

    class PlatformConfigurationSAXHandler extends DefaultHandler {
        private VFSMount currentMount = null;

        public void startElement(String uri, String localName, String qname,
            Attributes attrs) throws SAXException {
            if (currentElement == null) {
                if (qname.equals(PLATFORM_ELEMENT)) {
                    currentElement = qname;
                }

                nativeProcessProvider = null;
                nativeAuthenticationProvider = null;
                nativeSettings.clear();
            } else {
                if (currentElement.equals(PLATFORM_ELEMENT)) {
                    if (!qname.equals(NATIVE_PROCESS_ELEMENT) &&
                            !qname.equals(NATIVE_AUTH_ELEMENT) &&
                            !qname.equals(NATIVE_SETTING_ELEMENT) &&
                            !qname.equals(VFSMOUNT_ELEMENT) &&
                            !qname.equals(VFSROOT_ELEMENT) &&
                            !qname.equals(NFS_ELEMENT)) {
                        throw new SAXException("Unexpected element " + qname);
                    }
                } else if (currentElement.equals(VFSMOUNT_ELEMENT)) {
                    if (!qname.equals(VFSPERMISSION_ELEMENT)) {
                        throw new SAXException("Unexpected element " + qname);
                    }
                } else {
                    throw new SAXException("Unexpected element " + qname);
                }

                currentElement = qname;

                if (qname.equals(NATIVE_SETTING_ELEMENT)) {
                    String name = attrs.getValue(NAME_ATTRIBUTE);
                    String value = attrs.getValue(VALUE_ATTRIBUTE);

                    if ((name == null) || (value == null)) {
                        throw new SAXException(
                            "Required attributes missing for NativeSetting element");
                    }

                    log.debug("NativeSetting " + name + "=" + value);
                    nativeSettings.put(name, value);
                }

                if (qname.equals(VFSPERMISSION_ELEMENT)) {
                    String name = attrs.getValue(NAME_ATTRIBUTE);
                    String permissions = attrs.getValue(PERMISSIONS_ATTRIBUTE);
                    currentMount.setPermissions(new VFSPermission(name,
                            permissions));
                }

                if (qname.equals(VFSMOUNT_ELEMENT)) {
                    String path = attrs.getValue(PATH_ATTRIBUTE);
                    String mount = attrs.getValue(MOUNT_ATTRIBUTE);
                    String permissions = attrs.getValue(PERMISSIONS_ATTRIBUTE);

                    if ((path != null) && (mount != null)) {
                        // verify the mount - must start with / and be unique
                        if (!mount.trim().equals("/")) {
                            try {
                                currentMount = new VFSMount(mount, path);

                                if (permissions == null) {
                                    currentMount.setPermissions(new VFSPermission(
                                            "default"));
                                } else {
                                    currentMount.setPermissions(new VFSPermission(
                                            "default", permissions));
                                }

                                if (!vfsMounts.containsKey(
                                            currentMount.getMount())) {
                                    vfsMounts.put(currentMount.getMount(),
                                        currentMount);
                                } else {
                                    throw new SAXException("The mount " +
                                        mount + " is already defined");
                                }
                            } catch (IOException ex1) {
                                throw new SAXException(
                                    "VFSMount element is invalid mount=" +
                                    mount + " path=" + path);
                            }
                        } else {
                            throw new SAXException(
                                "The root mount / cannot be configured, use <VFSRoot path=\"" +
                                path + "\"/> instead");
                        }
                    } else {
                        throw new SAXException("Required " + PATH_ATTRIBUTE +
                            " attribute for element <" + VFSMOUNT_ELEMENT +
                            "> is missing");
                    }
                }

                if (qname.equals(VFSROOT_ELEMENT)) {
                    if (vfsRoot != null) {
                        throw new SAXException(
                            "Only one VFSRoot can be defined");
                    }

                    String path = attrs.getValue(PATH_ATTRIBUTE);
                    String permissions = attrs.getValue(PERMISSIONS_ATTRIBUTE);

                    try {
                        vfsRoot = new VFSMount("/", path);

                        if (permissions != null) {
                            vfsRoot.setPermissions(new VFSPermission(
                                    "default", permissions));
                        } else {
                            vfsRoot.setPermissions(new VFSPermission("default"));
                        }

                        vfsRoot.setRoot(true);
                    } catch (IOException ex) {
                        throw new SAXException(
                            "VFSRoot element is invalid path=" + path);
                    }
                }
            }
        }

        public void characters(char[] ch, int start, int length)
            throws SAXException {
            if (currentElement == null) {
                throw new SAXException("Unexpected characters found");
            }

            if (currentElement.equals(NATIVE_AUTH_ELEMENT)) {
                nativeAuthenticationProvider = new String(ch, start, length).trim();
                log.debug("NativeAuthenticationProvider=" +
                    nativeAuthenticationProvider);

                return;
            }

            if (currentElement.equals(NATIVE_PROCESS_ELEMENT)) {
                nativeProcessProvider = new String(ch, start, length).trim();
                log.debug("NativeProcessProvider=" + nativeProcessProvider);

                return;
            }

            if (currentElement.equals(NFS_ELEMENT)) {
                nativeFileSystemProvider = new String(ch, start, length).trim();
                log.debug("NativeFileSystemProvider=" +
                    nativeFileSystemProvider);

                return;
            }
        }

        public void endElement(String uri, String localName, String qname)
            throws SAXException {
            if (currentElement == null) {
                throw new SAXException("Unexpected end element for " + qname);
            }

            if (!currentElement.equals(qname)) {
                throw new SAXException("Unexpected end element found");
            }

            if (currentElement.equals(PLATFORM_ELEMENT)) {
                currentElement = null;

                return;
            }

            if (currentElement.equals(VFSPERMISSION_ELEMENT)) {
                currentElement = VFSMOUNT_ELEMENT;
            } else if (!currentElement.equals(NATIVE_SETTING_ELEMENT) &&
                    !currentElement.equals(NATIVE_AUTH_ELEMENT) &&
                    !currentElement.equals(NATIVE_PROCESS_ELEMENT) &&
                    !currentElement.equals(NFS_ELEMENT) &&
                    !currentElement.equals(VFSMOUNT_ELEMENT) &&
                    !currentElement.equals(VFSROOT_ELEMENT)) {
                throw new SAXException("Unexpected end element for " + qname);
            } else {
                currentElement = PLATFORM_ELEMENT;
            }
        }
    }
}
