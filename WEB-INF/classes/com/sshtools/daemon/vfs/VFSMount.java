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
package com.sshtools.daemon.vfs;

import com.sshtools.j2ssh.configuration.*;

import java.io.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class VFSMount {
    private String mount;
    private String path;
    private HashMap acl = new HashMap();
    private boolean isroot = false;

    /**
 * Creates a new VFSMount object.
 *
 * @param mount
 * @param path
 *
 * @throws IOException
 */
    public VFSMount(String mount, String path) throws IOException {
        path.replace('\\', '/');

        // Replace any tokens
        int index = path.indexOf("%HOME%");

        if (index >= 0) {
            path = ((index > 0) ? path.substring(0, index) : "") +
                ConfigurationLoader.getHomeDirectory() +
                (((index + 6) < (path.length() - 1))
                ? path.substring(index +
                    ((path.charAt(index + 6) == '/') ? 7 : 6)) : "");
        }

        File f = new File(path);

        if (!f.exists()) {
            f.mkdirs();
        }

        if (!mount.trim().startsWith("/")) {
            this.mount = "/" + mount.trim();
        } else {
            this.mount = mount.trim();
        }

        this.path = f.getCanonicalPath();
    }

    /**
 *
 *
 * @return
 */
    public boolean isRoot() {
        return isroot;
    }

    /**
 *
 *
 * @param isroot
 */
    public void setRoot(boolean isroot) {
        this.isroot = isroot;
    }

    /**
 *
 *
 * @param permissions
 */
    public void setPermissions(VFSPermission permissions) {
        acl.put(permissions.getName(), permissions);
    }

    /**
 *
 *
 * @return
 */
    public String getPath() {
        return path.replace('\\', '/');
    }

    /**
 *
 *
 * @return
 */
    public String getMount() {
        return mount;
    }

    /**
 *
 *
 * @return
 */
    public Map getPermissions() {
        return acl;
    }
}
