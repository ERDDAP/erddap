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
package com.sshtools.daemon.platform;

import com.sshtools.daemon.configuration.*;

import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.sftp.*;

import org.apache.commons.logging.*;

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public abstract class NativeFileSystemProvider {
    private static Log log = LogFactory.getLog(NativeAuthenticationProvider.class);
    private static NativeFileSystemProvider instance;

    /**  */
    public static final int OPEN_READ = SftpSubsystemClient.OPEN_READ;

    /**  */
    public static final int OPEN_WRITE = SftpSubsystemClient.OPEN_WRITE;

    /**  */
    public static final int OPEN_APPEND = SftpSubsystemClient.OPEN_APPEND;

    /**  */
    public static final int OPEN_CREATE = SftpSubsystemClient.OPEN_CREATE;

    /**  */
    public static final int OPEN_TRUNCATE = SftpSubsystemClient.OPEN_TRUNCATE;

    /**  */
    public static final int OPEN_EXCLUSIVE = SftpSubsystemClient.OPEN_EXCLUSIVE;

    static {
        try {
            if (ConfigurationLoader.isConfigurationAvailable(
                        PlatformConfiguration.class)) {
                Class cls = ConfigurationLoader.getExtensionClass(((PlatformConfiguration) ConfigurationLoader.getConfiguration(
                            PlatformConfiguration.class)).getNativeFileSystemProvider());
                instance = (NativeFileSystemProvider) cls.newInstance();
            }
        } catch (Exception e) {
            log.error("Failed to load native file system provider", e);
            instance = null;
        }
    }

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract boolean fileExists(String path) throws IOException;

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract String getCanonicalPath(String path)
        throws IOException, FileNotFoundException;

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws FileNotFoundException
 */
    public abstract String getRealPath(String path)
        throws FileNotFoundException;

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract boolean makeDirectory(String path)
        throws PermissionDeniedException, FileNotFoundException, IOException;

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws IOException
 * @throws FileNotFoundException
 */
    public abstract FileAttributes getFileAttributes(String path)
        throws IOException, FileNotFoundException;

    /**
 *
 *
 * @param handle
 *
 * @return
 *
 * @throws IOException
 * @throws InvalidHandleException
 */
    public abstract FileAttributes getFileAttributes(byte[] handle)
        throws IOException, InvalidHandleException;

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract byte[] openDirectory(String path)
        throws PermissionDeniedException, FileNotFoundException, IOException;

    /**
 *
 *
 * @param handle
 *
 * @return
 *
 * @throws InvalidHandleException
 * @throws EOFException
 */
    public abstract SftpFile[] readDirectory(byte[] handle)
        throws InvalidHandleException, EOFException, IOException;

    /**
 *
 *
 * @param path
 * @param flags
 * @param attrs
 *
 * @return
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract byte[] openFile(String path, UnsignedInteger32 flags,
        FileAttributes attrs)
        throws PermissionDeniedException, FileNotFoundException, IOException;

    /**
 *
 *
 * @param handle
 * @param offset
 * @param len
 *
 * @return
 *
 * @throws InvalidHandleException
 * @throws EOFException
 * @throws IOException
 */
    public abstract byte[] readFile(byte[] handle, UnsignedInteger64 offset,
        UnsignedInteger32 len)
        throws InvalidHandleException, EOFException, IOException;

    /**
 *
 *
 * @param handle
 * @param offset
 * @param data
 * @param off
 * @param len
 *
 * @throws InvalidHandleException
 * @throws IOException
 */
    public abstract void writeFile(byte[] handle, UnsignedInteger64 offset,
        byte[] data, int off, int len)
        throws InvalidHandleException, IOException;

    /**
 *
 *
 * @param handle
 *
 * @throws InvalidHandleException
 * @throws IOException
 */
    public abstract void closeFile(byte[] handle)
        throws InvalidHandleException, IOException;

    /**
 *
 *
 * @param path
 *
 * @throws PermissionDeniedException
 * @throws IOException
 * @throws FileNotFoundException
 */
    public abstract void removeFile(String path)
        throws PermissionDeniedException, IOException, FileNotFoundException;

    /**
 *
 *
 * @param oldpath
 * @param newpath
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract void renameFile(String oldpath, String newpath)
        throws PermissionDeniedException, FileNotFoundException, IOException;

    /**
 *
 *
 * @param path
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract void removeDirectory(String path)
        throws PermissionDeniedException, FileNotFoundException, IOException;

    /**
 *
 *
 * @param path
 * @param attrs
 *
 * @throws PermissionDeniedException
 * @throws IOException
 * @throws FileNotFoundException
 */
    public abstract void setFileAttributes(String path, FileAttributes attrs)
        throws PermissionDeniedException, IOException, FileNotFoundException;

    /**
 *
 *
 * @param handle
 * @param attrs
 *
 * @throws PermissionDeniedException
 * @throws IOException
 * @throws InvalidHandleException
 */
    public abstract void setFileAttributes(byte[] handle, FileAttributes attrs)
        throws PermissionDeniedException, IOException, InvalidHandleException;

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws UnsupportedFileOperationException
 * @throws FileNotFoundException
 * @throws IOException
 * @throws PermissionDeniedException
 */
    public abstract SftpFile readSymbolicLink(String path)
        throws UnsupportedFileOperationException, FileNotFoundException, 
            IOException, PermissionDeniedException;

    /**
 *
 *
 * @param link
 * @param target
 *
 * @throws UnsupportedFileOperationException
 * @throws FileNotFoundException
 * @throws IOException
 * @throws PermissionDeniedException
 */
    public abstract void createSymbolicLink(String link, String target)
        throws UnsupportedFileOperationException, FileNotFoundException, 
            IOException, PermissionDeniedException;

    public abstract String getDefaultPath(String username)
        throws FileNotFoundException;

    /**
 *
 *
 * @param username
 * @param path
 * @param permissions
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public abstract void verifyPermissions(String username, String path,
        String permissions)
        throws PermissionDeniedException, FileNotFoundException, IOException;

    /**
 *
 *
 * @return
 */
    public static NativeFileSystemProvider getInstance() {
        return instance;
    }
}
