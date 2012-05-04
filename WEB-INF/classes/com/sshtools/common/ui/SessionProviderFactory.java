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
package com.sshtools.common.ui;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.io.IOUtil;
import com.sshtools.j2ssh.util.ExtensionClassLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;


/**
 *
 * <p>This class is responsible for dynamically loading all the installed
 * session providers. A session provider can be used with
 * <code>SessionProviderFrame</code> to integrate an ssh service such as
 * a terminal window or sftp window within another application.</p>
 *
 * <p>To install a session provider you should provide a special properties
 * file resource at the root of your source tree called 'session.provider'.</p>
 *
 * <blockquote><pre>
 * This properties file should have the following properties defined:
 *
 * provider.id= [The unique name of the provider e.g 'sshterm']
     * provider.name= [The descriptive name of the provider e.g. 'Terminal Session']
 * provider.class= [Fully qualified classname of the provider implementation]
 * provider.shortdesc= [A short description of the provider]
 * provider.smallicon= [The providers small icon, must be filename only and be
 * placed in the same package as the provider class implementation]
 * provider.largeicon= [The providers large icon, must be filename only and be
 * placed in the same package as the provider class implementation]
 * provider.mnemonic= [The mnemonic character]
 * provider.options= [The options panel implementation, must implement
 * com.sshtools.common.ui.OptionsTab]
 * property.page.1= [An number of property page panels, must implement
 * com.sshtools.common.ui.SshToolsConnectionTab]
 * property.page.2= [More property pages added like this]
 * provider.weight= [Weight setting, used to order providers]
 * </pre></blockquote>
 *
 * </p>
 * @author Lee David Painter
 * @version $Id: SessionProviderFactory.java,v 1.12 2003/09/22 15:57:57 martianx Exp $
 */
public class SessionProviderFactory {
    private static Log log = LogFactory.getLog(SessionProviderFactory.class);
    private static SessionProviderFactory instance;
    HashMap providers = new HashMap();

    SessionProviderFactory() {
        ExtensionClassLoader classloader = ConfigurationLoader.getExtensionClassLoader();

        try {
            Enumeration enum = classloader.getResources("session.provider");
            URL url = null;
            Properties properties;
            InputStream in;
            SessionProvider provider;
            String name;
            String id;

            while (enum.hasMoreElements()) {
                try {
                    url = (URL) enum.nextElement();
                    in = url.openStream();
                    properties = new Properties();
                    properties.load(in);
                    IOUtil.closeStream(in);

                    if (properties.containsKey("provider.class") &&
                            properties.containsKey("provider.name")) {
                        Class cls = classloader.loadClass(properties.getProperty(
                                    "provider.class"));
                        String optionsClassName = properties.getProperty(
                                "provider.options");
                        Class optionsClass = ((optionsClassName == null) ||
                            optionsClassName.equals("")) ? null
                                                         : classloader.loadClass(optionsClassName);
                        String pageclass;
                        int num = 1;
                        Vector pages = new Vector();

                        do {
                            pageclass = properties.getProperty("property.page." +
                                    String.valueOf(num), null);

                            if (pageclass != null) {
                                pages.add(classloader.loadClass(pageclass));
                                num++;
                            }
                        } while (pageclass != null);

                        Class[] propertypages = new Class[pages.size()];
                        pages.toArray(propertypages);
                        name = properties.getProperty("provider.name");

                        int weight = Integer.parseInt(properties.getProperty(
                                    "provider.weight"));
                        id = properties.getProperty("provider.id", name);
                        provider = new SessionProvider(id, name, cls,
                                properties.getProperty("provider.shortdesc"),
                                properties.getProperty("provider.mnemonic"),
                                properties.getProperty("provider.smallicon"),
                                properties.getProperty("provider.largeicon"),
                                optionsClass, propertypages, weight);
                        providers.put(id, provider);
                        log.info("Installed " + provider.getName() +
                            " session provider");
                    }
                } catch (ClassNotFoundException ex) {
                    log.warn("Session provider class not found", ex);
                } catch (IOException ex) {
                    log.warn("Failed to read " + url.toExternalForm(), ex);
                }
            }
        } catch (IOException ex) {
        }
    }

    /**
* Get all the installed SessionProvider's.
*
* @return A list containing instances of <code>SessionProvider</code>
*/
    public List getSessionProviders() {
        return new Vector(providers.values());
    }

    /**
* <p>Get a <code>SessionProvider</code> by its id. The id is defined by the
 * provider.id property in the providers 'session.provider' resource file.</p>
*
* <p>Session providers that are currently defined within the SSHTools source
* tree are:<br>
* <blockquote><pre>
* sshterm   - Terminal session provider
* shift     - SFTP session provider
* tunneling - Secure port forwarding provider
* sshvnc    - VNC session provider
* </pre></blockquote>
*
* @param id the id of the SessionProvider.
* @return
*/
    public SessionProvider getProvider(String id) {
        return (SessionProvider) providers.get(id);
    }

    /**
* Get the one time instance of the factory.
* @return
*/
    public static SessionProviderFactory getInstance() {
        return (instance == null) ? (instance = new SessionProviderFactory())
                                  : instance;
    }
}
