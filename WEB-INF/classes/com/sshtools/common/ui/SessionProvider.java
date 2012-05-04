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


/**
 * <p>Instances of this class are created by the <code>SessionProviderFactory</code>
 * for each installed session provider. Instances of this class can be supplied
 * to the <code>SessionProviderFrame</code> to create windows contains the
 * session providers service</p>
 *
 * @author Lee David Painter
 * @version $Id: SessionProvider.java,v 1.12 2003/09/22 15:57:57 martianx Exp $
 */
public class SessionProvider {
    String id;
    String name;
    Class cls;
    String description;
    char mnemonic;
    ResourceIcon smallicon;
    ResourceIcon largeicon;
    Class[] propertypages;
    int weight;
    Class optionsClass;

    SessionProvider(String id, String name, Class cls, String description,
        String mnemonic, String smallicon, String largeicon,
        Class optionsClass, Class[] propertypages, int weight) {
        this.id = id;
        this.name = name;
        this.cls = cls;
        this.description = description;
        this.mnemonic = mnemonic.charAt(0);
        this.propertypages = propertypages;
        this.optionsClass = optionsClass;
        this.smallicon = new ResourceIcon(cls, smallicon);
        this.largeicon = new ResourceIcon(cls, largeicon);
        this.weight = weight;
    }

    /**
* Get the name of the provider e.g. 'Terminal Session'.
* @return
*/
    public String getName() {
        return name;
    }

    /**
* Get the class instance for the session providers implementation.
* @return
*/
    public Class getProviderClass() {
        return cls;
    }

    /**
* Get an array of class instances for the providers property pages.
* @return
*/
    public Class[] getPropertyPages() {
        return propertypages;
    }

    /**
* Get the description of the provider e.g. 'Opens a terminal session'
* @return
*/
    public String getDescription() {
        return description;
    }

    /**
* Get the mnemonic character for key access
* @return
*/
    public char getMnemonic() {
        return mnemonic;
    }

    /**
* Get the weight of the provider.
* @return
*/
    public int getWeight() {
        return weight;
    }

    /**
* Get the id of the provider e.g. 'sshterm'.
* @return
*/
    public String getId() {
        return id;
    }

    /**
* Get the small icon of the provider.
* @return
*/
    public ResourceIcon getSmallIcon() {
        return smallicon;
    }

    /**
* Get the large icon of the provider.
* @return
*/
    public ResourceIcon getLargeIcon() {
        return largeicon;
    }

    /**
* Get the options class implementation
* @return
*/
    public Class getOptionsClass() {
        return optionsClass;
    }

    /**
* Compares this session provider against another object. This method
* will only return true if the object provided is an instance of
* <code>SessionProvider</code> and that the provider id and implementation
* class are equal.
*
* @param obj
* @return
*/
    public boolean equals(Object obj) {
        if ((obj != null) && obj instanceof SessionProvider) {
            SessionProvider provider = (SessionProvider) obj;

            return provider.id.equals(id) &&
            provider.getProviderClass().equals(cls);
        }

        return false;
    }

    /**
* Returns the name of the provider.
* @return
*/
    public String toString() {
        return name;
    }
}
