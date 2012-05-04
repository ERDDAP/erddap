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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.Image;
import java.awt.Toolkit;

import java.io.FilePermission;

import java.net.URL;

import java.security.AccessControlException;
import java.security.AccessController;

import javax.swing.ImageIcon;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.20 $
 */
public class ResourceIcon extends ImageIcon {
    private static Log log = LogFactory.getLog(ResourceIcon.class.getName());
    Class cls;

    /**
* Creates a new ResourceIcon object.
*
* @param cls
* @param image
*/
    public ResourceIcon(Class cls, String image) {
        super();
        this.cls = cls;

        if (image.startsWith("/")) {
            loadImage(image);
        } else {
            String path = "/" + cls.getPackage().getName();
            path = path.replace('.', '/');
            path += ("/" + image);
            loadImage(path);
        }
    }

    /**
* Creates a new ResourceIcon object.
*
* @param url
*/
    public ResourceIcon(URL url) {
        super(url);
    }

    /**
* Creates a new ResourceIcon object.
*
* @param imageName
* @deprecated Having this available is now bad practice since most of our
* software is plugable; each class requesting a resource should do so from
* the class loader that loaded the class, to keep track of images a class
* should also not be requesting a resource that is outside its own package.
*
* For resources outside of a package, we should think about creating static
* helper class to store them.
*
 * Use the ResourceIcon(Class cls, String image) constructor instead providing
* the class instance of the class using the image.
*
*/
    public ResourceIcon(String imageName) {
        super();
        this.cls = getClass();
        loadImage(imageName);
    }

    /**
*
*
* @param imageName
*/
    protected void loadImage(String imageName) {
        Image image = null;
        URL url = cls.getResource(imageName);

        if (url != null) {
            log.debug(url.toString());
            image = Toolkit.getDefaultToolkit().getImage(url);
        } else {
            try {
                if (System.getSecurityManager() != null) {
                    AccessController.checkPermission(new FilePermission(
                            imageName, "read"));
                }

                image = Toolkit.getDefaultToolkit().getImage(imageName);
            } catch (AccessControlException ace) {
                log.error("Icon " + imageName + " could not be located as a " +
                    "resource, and the current security manager will not " +
                    "allow checking for a local file.");
            }
        }

        if (image != null) {
            this.setImage(image);
        }
    }
}
