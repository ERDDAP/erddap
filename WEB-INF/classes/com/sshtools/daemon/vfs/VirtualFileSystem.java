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

import com.sshtools.daemon.configuration.PlatformConfiguration;
import com.sshtools.daemon.platform.InvalidHandleException;
import com.sshtools.daemon.platform.NativeAuthenticationProvider;
import com.sshtools.daemon.platform.NativeFileSystemProvider;
import com.sshtools.daemon.platform.PermissionDeniedException;
import com.sshtools.daemon.platform.UnsupportedFileOperationException;

import com.sshtools.j2ssh.SshThread;
import com.sshtools.j2ssh.configuration.ConfigurationException;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.io.UnsignedInteger32;
import com.sshtools.j2ssh.io.UnsignedInteger64;
import com.sshtools.j2ssh.sftp.FileAttributes;
import com.sshtools.j2ssh.sftp.SftpFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.32 $
 */
public class VirtualFileSystem extends NativeFileSystemProvider {
    private static String USER_HOME = "/home/";
    private static Map vfsmounts;
    private static VFSMount vfsroot;
    private static Log log = LogFactory.getLog(VirtualFileSystem.class);
    private static VFSPermissionHandler permissionHandler = null;

    static {
        try {
            vfsmounts = ((PlatformConfiguration) ConfigurationLoader.getConfiguration(PlatformConfiguration.class)).getVFSMounts();
            vfsroot = ((PlatformConfiguration) ConfigurationLoader.getConfiguration(PlatformConfiguration.class)).getVFSRoot();
        } catch (ConfigurationException ex) {
            log.error("Failed to initialize the Virtual File System", ex);
        }
    }

    private Map openFiles = new HashMap();

    /**
 * Creates a new VirtualFileSystem object.
 *
 * @throws IOException
 */
    public VirtualFileSystem() throws IOException {
        if (!ConfigurationLoader.isConfigurationAvailable(
                    PlatformConfiguration.class)) {
            throw new IOException("No valid platform configuration available");
        }
    }

    public static void setPermissionHandler(
        VFSPermissionHandler permissionHandler) {
        VirtualFileSystem.permissionHandler = permissionHandler;
    }

    private static String getVFSHomeDirectory(String username)
        throws FileNotFoundException {
        if (permissionHandler != null) {
            return permissionHandler.getVFSHomeDirectory(username);
        } else {
            return USER_HOME + username;
        }
    }

    private static String getNFSHomeDirectory() throws FileNotFoundException {
        try {
            if (Thread.currentThread() instanceof SshThread &&
                    SshThread.hasUserContext()) {
                NativeAuthenticationProvider nap = NativeAuthenticationProvider.getInstance();

                return nap.getHomeDirectory(SshThread.getCurrentThreadUser());
            } else {
                throw new FileNotFoundException("There is no user logged in");
            }
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    /**
 *
 *
 * @param str
 * @param with
 *
 * @return
 */
    public static boolean startsWithIgnoreCase(String str, String with) {
        return str.substring(0,
            (with.length() > str.length()) ? str.length() : with.length())
                  .equalsIgnoreCase(with);
    }

    /**
 *
 *
 * @param nfspath
 *
 * @return
 *
 * @throws FileNotFoundException
 */
    public static String translateNFSPath(String nfspath)
        throws FileNotFoundException {
        nfspath = nfspath.replace('\\', '/');

        // ./ means home
        if (nfspath.startsWith("./")) {
            nfspath = nfspath.substring(2);
        }

        //if (startsWithIgnoreCase(nfspath, nfshome)) {
        try {
            String nfshome = getNFSHomeDirectory().replace('\\', '/');
            nfshome = translateCanonicalPath(nfshome, nfshome);

            String vfshome = getVFSHomeDirectory(SshThread.getCurrentThreadUser());

            // First check for the userhome
            log.debug("NFSPath=" + nfspath);
            log.debug("NFSHome=" + nfshome);
            nfspath = translateCanonicalPath(nfspath, nfshome);

            int idx = nfspath.indexOf(nfshome);

            return vfshome + nfspath.substring(nfshome.length());

            //            StringBuffer buf = new StringBuffer(nfspath);
            //            buf = buf.replace(idx, idx + nfshome.length(), vfshome);
            //            return buf.toString(); /*nfspath.replaceFirst(nfshome, vfshome);*/
            //}
        } catch (FileNotFoundException ex) { /* Ignore as we will try other mounts */
        }

        // Now lets translate from the available mounts
        Iterator it = vfsmounts.entrySet().iterator();
        Map.Entry entry;
        String mount;
        String path;
        VFSMount m;

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            mount = (String) entry.getKey();
            m = (VFSMount) entry.getValue();
            path = m.getPath();
            log.debug(m.getMount() + "=" + m.getPath());

            // if (startsWithIgnoreCase(nfspath, path)) {
            try {
                nfspath = translateCanonicalPath(nfspath, path);

                int idx = nfspath.indexOf(path);
                StringBuffer buf = new StringBuffer(nfspath);
                buf = buf.replace(idx, idx + path.length(), mount);

                return buf.toString();
            } catch (FileNotFoundException ex) {
                /* Ingore as we will try other mounts */
            }

            // }
        }

        // if (startsWithIgnoreCase(nfspath, vfsroot.getPath())) {
        log.debug("VFSRoot=" + vfsroot.getPath());
        nfspath = translateCanonicalPath(nfspath, vfsroot.getPath());
        path = nfspath.substring(vfsroot.getPath().length()); //replaceFirst(vfsroot.getPath(), "");

        return (path.startsWith("/") ? path : ("/" + path));

        //  } else {
        //      throw new FileNotFoundException(nfspath + " could not be found");
        //  }
    }

    private static VFSMount getMount(String vfspath)
        throws FileNotFoundException, IOException {
        String vfshome = getVFSHomeDirectory(SshThread.getCurrentThreadUser());
        VFSMount m;

        if (vfspath.startsWith("/")) {
            if (vfspath.startsWith(vfshome)) {
                m = new VFSMount(vfshome, getNFSHomeDirectory());
                m.setPermissions(new VFSPermission(
                        SshThread.getCurrentThreadUser(), "rwx"));

                return m;
            } else {
                Iterator it = vfsmounts.entrySet().iterator();
                Map.Entry entry;
                String mount;

                while (it.hasNext()) {
                    entry = (Map.Entry) it.next();
                    mount = (String) entry.getKey();

                    if (vfspath.startsWith(mount)) {
                        return (VFSMount) entry.getValue();
                    }
                }

                if (vfsroot != null) {
                    return vfsroot;
                } else {
                    throw new FileNotFoundException("The path was not found");
                }
            }
        } else {
            m = new VFSMount(vfshome, getNFSHomeDirectory());
            m.setPermissions(new VFSPermission(vfshome.substring(
                        vfshome.lastIndexOf("/")), "rwx"));

            return m;
        }
    }

    /**
 *
 *
 * @param vfspath
 *
 * @return
 *
 * @throws FileNotFoundException
 */
    public static String translateVFSPath(String vfspath)
        throws FileNotFoundException {
        return translateVFSPath(vfspath, null);
    }

    public static String translateVFSPath(String vfspath, String vfscwd)
        throws FileNotFoundException {
        // Translate any backslashes for sanity
        vfspath = vfspath.replace('\\', '/').trim();

        try {
            if (!vfspath.startsWith("/")) {
                // Work out the path using the current directory
                String path = (((vfscwd == null) || vfscwd.trim().equals(""))
                    ? getVFSHomeDirectory(SshThread.getCurrentThreadUser())
                    : vfscwd);
                vfspath = path + (path.endsWith("/") ? "" : "/") + vfspath;
            }

            String nfshome = getNFSHomeDirectory().replace('\\', '/');
            String vfshome = getVFSHomeDirectory(SshThread.getCurrentThreadUser());

            if (vfspath.startsWith(vfshome)) {
                // Return the canonicalized system dependent path
                if (vfspath.length() > vfshome.length()) {
                    return translateCanonicalPath(nfshome +
                        (nfshome.endsWith("/") ? "" : "/") +
                        vfspath.substring(vfshome.length() + 1), nfshome);
                } else {
                    return translateCanonicalPath(nfshome, nfshome);
                }
            }
        } catch (FileNotFoundException ex) {
            // Ignore since we dont always need to be running as a user
        }

        // The path does not refer to the absolute USER_HOME
        // so we will look up using the platform.xml VFS mounts
        Iterator it = vfsmounts.entrySet().iterator();
        Map.Entry entry;
        String mount;
        String path;
        VFSMount m;

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            mount = (String) entry.getKey();
            m = (VFSMount) entry.getValue();
            path = m.getPath();

            if (vfspath.startsWith(mount)) {
                // Lets translate the path, making sure we do not move outside
                // vfs with ..
                String str = path + vfspath.substring(mount.length());

                // vfspath.replaceFirst(mount,
                //        path)
                return translateCanonicalPath(str, path);
            }
        }

        // If we reached here then the VFS path did not refer to an optional mount
        // or the users home directory, so lets attempt to use the VFS root is there
        // is one defined
        if (vfsroot != null) {
            path = vfsroot.getPath() +
                (vfsroot.getPath().endsWith("/") ? vfspath.substring(1) : vfspath);

            return translateCanonicalPath(path, vfsroot.getPath());
        } else {
            throw new FileNotFoundException("The file could not be found");
        }
    }

    /*else {
  try {
    String nfshome = (nfscwd == null || nfscwd.trim().equals("") ? getNFSHomeDirectory() : nfscwd);
    String path = nfshome + (nfshome.endsWith("/") ? "" : "/") + vfspath;
    return translateCanonicalPath(path, nfshome);
  }
  catch (FileNotFoundException ex1) {
    throw new FileNotFoundException(
     "Only fully qualified VFS paths can be translated outside of a user context");
  }
  /*  String path = nfshome + (nfshome.endsWith("/") ? "" : "/")
         + vfspath;
     return translateCanonicalPath(path, nfshome);*/

    //}

    /**
 *
 *
 * @param path
 * @param securemount
 *
 * @return
 *
 * @throws FileNotFoundException
 */
    public static String translateCanonicalPath(String path, String securemount)
        throws FileNotFoundException {
        try {
            log.debug("Translating for canonical path " + path +
                " against secure mount " + securemount);

            File f = new File(path);
            String canonical = f.getCanonicalPath().replace('\\', '/');
            File f2 = new File(securemount);
            String canonical2 = f2.getCanonicalPath().replace('\\', '/');

            // Verify that the canonical path does not exit out of the mount
            if (canonical.startsWith(canonical2)) {
                return canonical;
            } else {
                throw new FileNotFoundException(path + " could not be found");
            }
        } catch (IOException ex) {
            throw new FileNotFoundException(path + " could not be found");
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
    public boolean makeDirectory(String path)
        throws PermissionDeniedException, FileNotFoundException, IOException {
        //    String realPath = path;
        path = VirtualFileSystem.translateVFSPath(path);

        File f = new File(path);
        verifyPermissions(SshThread.getCurrentThreadUser(), path, "rw");
        log.debug("Creating directory " + f.getAbsolutePath());

        return f.mkdir();
    }

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws FileNotFoundException
 * @throws IOException
 */
    public VFSPermission getVFSPermission(String path)
        throws FileNotFoundException, IOException {
        VFSMount mount = getMount(translateNFSPath(path));

        if (mount.getPermissions().containsKey(SshThread.getCurrentThreadUser())) {
            return (VFSPermission) mount.getPermissions().get(SshThread.getCurrentThreadUser());
        } else {
            return (VFSPermission) mount.getPermissions().get("default");
        }
    }

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
    public FileAttributes getFileAttributes(byte[] handle)
        throws IOException, InvalidHandleException {
        String shandle = new String(handle);

        if (openFiles.containsKey(shandle)) {
            Object obj = openFiles.get(shandle);
            File f;

            if (obj instanceof OpenFile) {
                f = ((OpenFile) obj).getFile();
            } else if (obj instanceof OpenDirectory) {
                f = ((OpenDirectory) obj).getFile();
            } else {
                throw new IOException("Unexpected open file handle");
            }

            VFSPermission permissions = getVFSPermission(f.getAbsolutePath());

            if (permissions == null) {
                throw new IOException("No default permissions set");
            }

            FileAttributes attrs = new FileAttributes();
            attrs.setSize(new UnsignedInteger64(String.valueOf(f.length())));
            attrs.setTimes(new UnsignedInteger32(f.lastModified() / 1000),
                new UnsignedInteger32(f.lastModified() / 1000));

            boolean canExec = true;

            try {
                if (System.getSecurityManager() != null) {
                    System.getSecurityManager().checkExec(f.getCanonicalPath());
                }
            } catch (SecurityException ex1) {
                canExec = false;
            }

            attrs.setPermissions((((f.canRead() && permissions.canRead()) ? "r"
                                                                          : "-") +
                ((f.canWrite() && permissions.canWrite()) ? "w" : "-") +
                ((canExec && permissions.canExecute()) ? "x" : "-")));
            attrs.setPermissions(new UnsignedInteger32(attrs.getPermissions()
                                                            .longValue() |
                    (f.isDirectory() ? FileAttributes.S_IFDIR
                                     : FileAttributes.S_IFREG)));

            return attrs;
        } else {
            throw new InvalidHandleException("The handle is invalid");
        }
    }

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
    public FileAttributes getFileAttributes(String path)
        throws IOException, FileNotFoundException {
        log.debug("Getting file attributes for " + path);
        path = translateVFSPath(path);

        // Look up the VFS mount attributes
        File f = new File(path);
        path = f.getCanonicalPath();

        if (!f.exists()) {
            throw new FileNotFoundException(path + " doesn't exist");
        }

        VFSPermission permissions = getVFSPermission(path);

        if (permissions == null) {
            throw new IOException("No default permissions set");
        }

        FileAttributes attrs = new FileAttributes();
        attrs.setSize(new UnsignedInteger64(String.valueOf(f.length())));
        attrs.setTimes(new UnsignedInteger32(f.lastModified() / 1000),
            new UnsignedInteger32(f.lastModified() / 1000));

        boolean canExec = true;

        try {
            if (System.getSecurityManager() != null) {
                System.getSecurityManager().checkExec(f.getCanonicalPath());
            }
        } catch (SecurityException ex1) {
            canExec = false;
        }

        attrs.setPermissions((((f.canRead() && permissions.canRead()) ? "r" : "-") +
            ((f.canWrite() && permissions.canWrite()) ? "w" : "-") +
            ((canExec && permissions.canExecute()) ? "x" : "-")));
        attrs.setPermissions(new UnsignedInteger32(attrs.getPermissions()
                                                        .longValue() |
                (f.isDirectory() ? FileAttributes.S_IFDIR : FileAttributes.S_IFREG)));

        return attrs;
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
    public byte[] openDirectory(String path)
        throws PermissionDeniedException, FileNotFoundException, IOException {
        String realPath = path;
        path = VirtualFileSystem.translateVFSPath(path);

        File f = new File(path);
        verifyPermissions(SshThread.getCurrentThreadUser(), path, "r");

        if (f.exists()) {
            if (f.isDirectory()) {
                openFiles.put(f.toString(), new OpenDirectory(realPath, path, f));

                return f.toString().getBytes("US-ASCII");
            } else {
                throw new IOException(translateNFSPath(path) +
                    " is not a directory");
            }
        } else {
            throw new FileNotFoundException(translateNFSPath(path) +
                " does not exist");
        }
    }

    /**
 *
 *
 * @param handle
 *
 * @return
 *
 * @throws InvalidHandleException
 * @throws EOFException
 * @throws IOException
 */
    public SftpFile[] readDirectory(byte[] handle)
        throws InvalidHandleException, EOFException, IOException {
        String shandle = new String(handle);

        if (openFiles.containsKey(shandle)) {
            Object obj = openFiles.get(shandle);

            if (obj instanceof OpenDirectory) {
                OpenDirectory dir = (OpenDirectory) obj;
                int pos = dir.getPosition();
                File[] children = dir.getChildren();

                if (children == null) {
                    throw new IOException("Permission denined.");
                }

                int count = ((children.length - pos) < 100)
                    ? (children.length - pos) : 100;

                if (count > 0) {
                    SftpFile[] files = new SftpFile[count];

                    for (int i = 0; i < files.length; i++) {
                        File f = children[pos + i];
                        String absolutePath = dir.realPath + "/" + f.getName();
                        SftpFile sftpfile = new SftpFile(f.getName(),
                                getFileAttributes(absolutePath));
                        files[i] = sftpfile;
                    }

                    dir.readpos = pos + files.length;

                    return files;
                } else {
                    throw new EOFException("There are no more files");
                }
            } else {
                throw new InvalidHandleException(
                    "Handle is not an open directory");
            }
        } else {
            throw new InvalidHandleException("The handle is invalid");
        }
    }

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
    public byte[] openFile(String path, UnsignedInteger32 flags,
        FileAttributes attrs)
        throws PermissionDeniedException, FileNotFoundException, IOException {
        path = VirtualFileSystem.translateVFSPath(path);

        File f = new File(path);
        verifyPermissions(SshThread.getCurrentThreadUser(), path, "r");

        // Check if the file does not exist and process according to flags
        if (!f.exists()) {
            if ((flags.intValue() & NativeFileSystemProvider.OPEN_CREATE) == NativeFileSystemProvider.OPEN_CREATE) {
                // The file does not exist and the create flag is present so lets create it
                if (!f.createNewFile()) {
                    throw new IOException(translateNFSPath(path) +
                        " could not be created");
                }
            } else {
                // The file does not exist and no create flag present
                throw new FileNotFoundException(translateNFSPath(path) +
                    " does not exist");
            }
        } else {
            if (((flags.intValue() & NativeFileSystemProvider.OPEN_CREATE) == NativeFileSystemProvider.OPEN_CREATE) &&
                    ((flags.intValue() &
                    NativeFileSystemProvider.OPEN_EXCLUSIVE) == NativeFileSystemProvider.OPEN_EXCLUSIVE)) {
                // The file exists but the EXCL flag is set which requires that the
                // file should not exist prior to creation, so throw a status exception
                throw new IOException(translateNFSPath(path) +
                    " already exists");
            }
        }

        // The file now exists so open the file according to the flags yb building the relevant
        // flags for the RandomAccessFile class
        String mode = "r" +
            (((flags.intValue() & NativeFileSystemProvider.OPEN_WRITE) == NativeFileSystemProvider.OPEN_WRITE)
            ? "ws" : "");
        RandomAccessFile raf = new RandomAccessFile(f, mode);

        // Determine whether we need to truncate the file
        if (((flags.intValue() & NativeFileSystemProvider.OPEN_CREATE) == NativeFileSystemProvider.OPEN_CREATE) &&
                ((flags.intValue() & NativeFileSystemProvider.OPEN_TRUNCATE) == NativeFileSystemProvider.OPEN_TRUNCATE)) {
            // Set the length to zero
            raf.setLength(0);
        }

        // Record the open file
        openFiles.put(raf.toString(), new OpenFile(f, raf, flags));

        // Return the handle
        return raf.toString().getBytes("US-ASCII");
    }

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
    public byte[] readFile(byte[] handle, UnsignedInteger64 offset,
        UnsignedInteger32 len)
        throws InvalidHandleException, EOFException, IOException {
        String shandle = new String(handle);

        if (openFiles.containsKey(shandle)) {
            Object obj = openFiles.get(shandle);

            if (obj instanceof OpenFile) {
                OpenFile file = (OpenFile) obj;

                if ((file.getFlags().intValue() &
                        NativeFileSystemProvider.OPEN_READ) == NativeFileSystemProvider.OPEN_READ) {
                    byte[] buf = new byte[len.intValue()];

                    if (file.getRandomAccessFile().getFilePointer() != offset.longValue()) {
                        file.getRandomAccessFile().seek(offset.longValue());
                    }

                    int read = file.getRandomAccessFile().read(buf);

                    if (read >= 0) {
                        if (read == buf.length) {
                            return buf;
                        } else {
                            byte[] tmp = new byte[read];
                            System.arraycopy(buf, 0, tmp, 0, read);

                            return tmp;
                        }
                    } else {
                        throw new EOFException("The file is EOF");
                    }
                } else {
                    throw new InvalidHandleException(
                        "The file handle was not opened for reading");
                }
            } else {
                throw new InvalidHandleException("Handle is not an open file");
            }
        } else {
            throw new InvalidHandleException("The handle is invalid");
        }
    }

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
    public void writeFile(byte[] handle, UnsignedInteger64 offset, byte[] data,
        int off, int len) throws InvalidHandleException, IOException {
        String shandle = new String(handle);

        if (openFiles.containsKey(shandle)) {
            Object obj = openFiles.get(shandle);

            if (obj instanceof OpenFile) {
                OpenFile file = (OpenFile) obj;

                if ((file.getFlags().intValue() &
                        NativeFileSystemProvider.OPEN_WRITE) == NativeFileSystemProvider.OPEN_WRITE) {
                    if ((file.getFlags().intValue() &
                            NativeFileSystemProvider.OPEN_APPEND) == NativeFileSystemProvider.OPEN_APPEND) {
                        // Force the data to be written to the end of the file by seeking to the end
                        file.getRandomAccessFile().seek(file.getRandomAccessFile()
                                                            .length());
                    } else if (file.getRandomAccessFile().getFilePointer() != offset.longValue()) {
                        // Move the file pointer if its not in the write place
                        file.getRandomAccessFile().seek(offset.longValue());
                    }

                    file.getRandomAccessFile().write(data, off, len);
                } else {
                    throw new InvalidHandleException(
                        "The file was not opened for writing");
                }
            } else {
                throw new InvalidHandleException("Handle is not an open file");
            }
        } else {
            throw new InvalidHandleException("The handle is invalid");
        }
    }

    /**
 *
 *
 * @param handle
 *
 * @throws InvalidHandleException
 * @throws IOException
 */
    public void closeFile(byte[] handle)
        throws InvalidHandleException, IOException {
        String shandle = new String(handle);

        if (openFiles.containsKey(shandle)) {
            Object obj = openFiles.get(shandle);

            if (obj instanceof OpenDirectory) {
                openFiles.remove(shandle);
            } else if (obj instanceof OpenFile) {
                ((OpenFile) obj).getRandomAccessFile().close();
                openFiles.remove(shandle);
            } else {
                throw new InvalidHandleException("Internal server error");
            }
        } else {
            throw new InvalidHandleException("The handle is invalid");
        }
    }

    /**
 *
 *
 * @param path
 *
 * @throws PermissionDeniedException
 * @throws IOException
 * @throws FileNotFoundException
 */
    public void removeFile(String path)
        throws PermissionDeniedException, IOException, FileNotFoundException {
        path = VirtualFileSystem.translateVFSPath(path);

        File f = new File(path);

        if (f.exists()) {
            try {
                if (f.isFile()) {
                    if (!f.delete()) {
                        throw new IOException("Failed to delete " +
                            translateNFSPath(path));
                    }
                } else {
                    throw new IOException(translateNFSPath(path) +
                        " is a directory, use remove directory command to remove");
                }
            } catch (SecurityException se) {
                throw new PermissionDeniedException("Permission denied");
            }
        } else {
            throw new FileNotFoundException(translateNFSPath(path) +
                " does not exist");
        }
    }

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
    public void renameFile(String oldpath, String newpath)
        throws PermissionDeniedException, FileNotFoundException, IOException {
        oldpath = VirtualFileSystem.translateVFSPath(oldpath);
        newpath = VirtualFileSystem.translateVFSPath(newpath);

        File f = new File(oldpath);
        verifyPermissions(SshThread.getCurrentThreadUser(), oldpath, "rw");
        verifyPermissions(SshThread.getCurrentThreadUser(), newpath, "rw");

        if (f.exists()) {
            File f2 = new File(newpath);

            if (!f2.exists()) {
                if (!f.renameTo(f2)) {
                    throw new IOException("Failed to rename file " +
                        translateNFSPath(oldpath));
                }
            } else {
                throw new IOException(translateNFSPath(newpath) +
                    " already exists");
            }
        } else {
            throw new FileNotFoundException(translateNFSPath(oldpath) +
                " does not exist");
        }
    }

    /**
 *
 *
 * @param path
 *
 * @throws PermissionDeniedException
 * @throws FileNotFoundException
 * @throws IOException
 */
    public void removeDirectory(String path)
        throws PermissionDeniedException, FileNotFoundException, IOException {
        path = VirtualFileSystem.translateVFSPath(path);

        File f = new File(path);
        verifyPermissions(SshThread.getCurrentThreadUser(), path, "rw");

        if (f.isDirectory()) {
            if (f.exists()) {
                if (f.listFiles().length == 0) {
                    if (!f.delete()) {
                        throw new IOException("Failed to remove directory " +
                            translateNFSPath(path));
                    }
                } else {
                    throw new IOException(translateNFSPath(path) +
                        " is not an empty directory");
                }
            } else {
                throw new FileNotFoundException(translateNFSPath(path) +
                    " does not exist");
            }
        } else {
            throw new IOException(translateNFSPath(path) +
                " is not a directory");
        }
    }

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
    public void setFileAttributes(String path, FileAttributes attrs)
        throws PermissionDeniedException, IOException, FileNotFoundException {
        // Since we cannot really set permissions, this should be ignored as we
        // do not want applications to fail.

        /*String realPath = VirtualFileSystem.translateVFSPath(path);
     throw new PermissionDeniedException(
    "Cannot set file attributes using virtual file system for file "
    + realPath);*/
    }

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
    public void setFileAttributes(byte[] handle, FileAttributes attrs)
        throws PermissionDeniedException, IOException, InvalidHandleException {
    }

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
    public SftpFile readSymbolicLink(String path)
        throws UnsupportedFileOperationException, FileNotFoundException, 
            IOException, PermissionDeniedException {
        throw new UnsupportedFileOperationException(
            "Symbolic links are not supported by the Virtual File System");
    }

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
    public void createSymbolicLink(String link, String target)
        throws UnsupportedFileOperationException, FileNotFoundException, 
            IOException, PermissionDeniedException {
        throw new UnsupportedFileOperationException(
            "Symbolic links are not supported by the Virtual File System");
    }

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws IOException
 */
    public boolean fileExists(String path) throws IOException {
        File f = new File(VirtualFileSystem.translateVFSPath(path));

        return f.exists();
    }

    public String getDefaultPath(String username) throws FileNotFoundException {
        return getVFSHomeDirectory(username);
    }

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
    public String getCanonicalPath(String path)
        throws IOException, FileNotFoundException {
        File f = new File(VirtualFileSystem.translateVFSPath(path));

        return f.getCanonicalPath();
    }

    /**
 *
 *
 * @param path
 *
 * @return
 *
 * @throws FileNotFoundException
 */
    public String getRealPath(String path) throws FileNotFoundException {
        log.debug("Get real path for '" + path + "'");
        path = VirtualFileSystem.translateVFSPath(path);
        log.debug("Translated VFS is '" + path + "'");
        path = VirtualFileSystem.translateNFSPath(path);
        log.debug("Translated NFS is '" + path + "'");

        return path;
    }

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
    public void verifyPermissions(String username, String path,
        String permissions)
        throws PermissionDeniedException, FileNotFoundException, IOException {
        String vfspath = translateNFSPath(path);

        if (permissionHandler == null) {
            VFSMount mount = getMount(vfspath);
            VFSPermission perm;

            if (mount.getPermissions().containsKey(SshThread.getCurrentThreadUser())) {
                perm = (VFSPermission) mount.getPermissions().get(SshThread.getCurrentThreadUser());
            } else if (mount.getPermissions().containsKey("default")) {
                perm = (VFSPermission) mount.getPermissions().get("default");
            } else {
                throw new PermissionDeniedException(
                    "No permissions set for mount");
            }

            if (!perm.verifyPermissions(permissions)) {
                throw new PermissionDeniedException("Permission denied for " +
                    translateNFSPath(path));
            }
        } else {
            permissionHandler.verifyPermissions(username, path, permissions);
        }
    }

    class OpenFile {
        File f;
        RandomAccessFile raf;
        UnsignedInteger32 flags;

        public OpenFile(File f, RandomAccessFile raf, UnsignedInteger32 flags) {
            this.f = f;
            this.raf = raf;
            this.flags = flags;
        }

        public File getFile() {
            return f;
        }

        public RandomAccessFile getRandomAccessFile() {
            return raf;
        }

        public UnsignedInteger32 getFlags() {
            return flags;
        }
    }

    class OpenDirectory {
        File f;
        File[] children;
        int readpos = 0;
        String path;
        String realPath;

        public OpenDirectory(String realPath, String path, File f) {
            this.path = path;
            this.realPath = realPath;
            this.f = f;
            this.children = f.listFiles();
        }

        public File getFile() {
            return f;
        }

        public File[] getChildren() {
            return children;
        }

        public int getPosition() {
            return readpos;
        }

        public void setPosition(int readpos) {
            this.readpos = readpos;
        }
    }
}
