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
package com.sshtools.j2ssh;

import com.sshtools.j2ssh.connection.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.sftp.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * <p>
 * Implements a Secure File Transfer (SFTP) client.
 * </p>
 *
 * @author Lee David Painter
 * @version $Revision: 1.44 $
 *
 * @since 0.2.0
 */
public class SftpClient {
    SftpSubsystemClient sftp;
    String cwd;
    String lcwd;
    private int BLOCKSIZE = 65535;

    // Default permissions is determined by default_permissions ^ umask
    int umask = 0022;
    int default_permissions = 0777;

    /**
     * <p>
     * Constructs the SFTP client.
     * </p>
     *
     * @param ssh the <code>SshClient</code> instance
     *
     * @throws IOException if an IO error occurs
     */
    SftpClient(SshClient ssh) throws IOException {
        this(ssh, null);
    }

    /**
     * <p>
     * Constructs the SFTP client with a given channel event listener.
     * </p>
     *
     * @param ssh the <code>SshClient</code> instance
     * @param eventListener an event listener implementation
     *
     * @throws IOException if an IO error occurs
     */
    SftpClient(SshClient ssh, ChannelEventListener eventListener)
        throws IOException {
        if (!ssh.isConnected()) {
            throw new IOException("SshClient is not connected");
        }

        this.sftp = ssh.openSftpChannel(eventListener);

        // Get the users default directory
        cwd = sftp.getDefaultDirectory();
        lcwd = System.getProperty("user.home");
    }

    /**
     * Sets the umask used by this client.
     * @param umask
     * @return the previous umask value
     */
    public int umask(int umask) {
        int old = umask;
        this.umask = umask;

        return old;
    }

    /**
     * <p>
     * Changes the working directory on the remote server.
     * </p>
     *
     * @param dir the new working directory
     *
     * @throws IOException if an IO error occurs or the file does not exist
     * @throws FileNotFoundException
     *
     * @since 0.2.0
     */
    public void cd(String dir) throws IOException {
        try {
            String actual;

            if (dir.equals("")) {
                actual = sftp.getDefaultDirectory();
            } else {
                actual = resolveRemotePath(dir);
                actual = sftp.getAbsolutePath(actual);
            }

            FileAttributes attr = sftp.getAttributes(actual);

            if (!attr.isDirectory()) {
                throw new IOException(dir + " is not a directory");
            }

            cwd = actual;
        } catch (IOException ex) {
            throw new FileNotFoundException(dir + " could not be found");
        }
    }

    private File resolveLocalPath(String path) throws IOException {
        File f = new File(path);

        if (!f.isAbsolute()) {
            f = new File(lcwd, path);
        }

        return f;
    }

    private String resolveRemotePath(String path) throws IOException {
        verifyConnection();

        String actual;

        if (!path.startsWith("/")) {
            actual = cwd + (cwd.endsWith("/") ? "" : "/") + path;
        } else {
            actual = path;
        }

        return actual;
    }

    private void verifyConnection() throws SshException {
        if (sftp.isClosed()) {
            throw new SshException("The SFTP connection has been closed");
        }
    }

    /**
     * <p>
     * Creates a new directory on the remote server. This method will throw an
     * exception if the directory already exists. To create directories and
     * disregard any errors use the <code>mkdirs</code> method.
     * </p>
     *
     * @param dir the name of the new directory
     *
     * @throws IOException if an IO error occurs or if the directory already
     *         exists
     *
     * @since 0.2.0
     */
    public void mkdir(String dir) throws IOException {
        String actual = resolveRemotePath(dir);

        try {
            FileAttributes attrs = stat(actual);

            if (!attrs.isDirectory()) {
                throw new IOException("File already exists named " + dir);
            }
        } catch (IOException ex) {
            sftp.makeDirectory(actual);
            chmod(default_permissions ^ umask, actual);
        }
    }

    /**
     * <p>
     * Create a directory or set of directories. This method will not fail even
     * if the directories exist. It is advisable to test whether the directory
     * exists before attempting an operation by using the <code>stat</code>
     * method to return the directories attributes.
     * </p>
     *
     * @param dir the path of directories to create.
     */
    public void mkdirs(String dir) {
        StringTokenizer tokens = new StringTokenizer(dir, "/");
        String path = dir.startsWith("/") ? "/" : "";

        while (tokens.hasMoreElements()) {
            path += (String) tokens.nextElement();

            try {
                stat(path);
            } catch (IOException ex) {
                try {
                    mkdir(path);
                } catch (IOException ex2) {
                }
            }

            path += "/";
        }
    }

    /**
     * <p>
     * Returns the absolute path name of the current remote working directory.
     * </p>
     *
     * @return the absolute path of the remote working directory.
     *
     * @since 0.2.0
     */
    public String pwd() {
        return cwd;
    }

    /**
     * <p>
     * List the contents of the current remote working directory.
     * </p>
     *
     * <p>
     * Returns a list of <code>SftpFile</code> instances for the current
     * working directory.
     * </p>
     *
     * @return a list of SftpFile for the current working directory
     *
     * @throws IOException if an IO error occurs
     *
     * @see com.sshtools.j2ssh.sftp.SftpFile
     * @since 0.2.0
     */
    public List ls() throws IOException {
        return ls(cwd);
    }

    /**
     * <p>
     * List the contents remote directory.
     * </p>
     *
     * <p>
     * Returns a list of <code>SftpFile</code> instances for the remote
     * directory.
     * </p>
     *
     * @param path the path on the remote server to list
     *
     * @return a list of SftpFile for the remote directory
     *
     * @throws IOException if an IO error occurs
     *
     * @see com.sshtools.j2ssh.sftp.SftpFile
     * @since 0.2.0
     */
    public List ls(String path) throws IOException {
        String actual = resolveRemotePath(path);
        FileAttributes attrs = sftp.getAttributes(actual);

        if (!attrs.isDirectory()) {
            throw new IOException(path + " is not a directory");
        }

        SftpFile file = sftp.openDirectory(actual);
        Vector children = new Vector();

        while (sftp.listChildren(file, children) > -1) {
            ;
        }

        file.close();

        return children;
    }

    /**
     * <p>
     * Changes the local working directory.
     * </p>
     *
     * @param path the path to the new working directory
     *
     * @throws IOException if an IO error occurs
     *
     * @since 0.2.0
     */
    public void lcd(String path) throws IOException {
        File actual;

        if (!isLocalAbsolutePath(path)) {
            actual = new File(lcwd, path);
        } else {
            actual = new File(path);
        }

        if (!actual.isDirectory()) {
            throw new IOException(path + " is not a directory");
        }

        lcwd = actual.getCanonicalPath();
    }

    private static boolean isLocalAbsolutePath(String path) {
        return (new File(path)).isAbsolute();
    }

    /**
     * <p>
     * Returns the absolute path to the local working directory.
     * </p>
     *
     * @return the absolute path of the local working directory.
     *
     * @since 0.2.0
     */
    public String lpwd() {
        return lcwd;
    }

    /**
     * <p>
     * Download the remote file to the local computer.
     * </p>
     *
     * @param path the path to the remote file
     * @param progress
     *
     * @return
     *
     * @throws IOException if an IO error occurs of the file does not exist
     * @throws TransferCancelledException
     *
     * @since 0.2.0
     */
    public FileAttributes get(String path, FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        String localfile;

        if (path.lastIndexOf("/") > -1) {
            localfile = path.substring(path.lastIndexOf("/") + 1);
        } else {
            localfile = path;
        }

        return get(path, localfile, progress);
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
    public FileAttributes get(String path) throws IOException {
        return get(path, (FileTransferProgress) null);
    }

    private void transferFile(InputStream in, OutputStream out)
        throws IOException, TransferCancelledException {
        transferFile(in, out, null);
    }

    private void transferFile(InputStream in, OutputStream out,
        FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        try {
            long bytesSoFar = 0;
            byte[] buffer = new byte[BLOCKSIZE];
            int read;

            while ((read = in.read(buffer)) > -1) {
                if ((progress != null) && progress.isCancelled()) {
                    throw new TransferCancelledException();
                }

                if (read > 0) {
                    out.write(buffer, 0, read);

                    //out.flush();
                    bytesSoFar += read;

                    if (progress != null) {
                        progress.progressed(bytesSoFar);
                    }
                }
            }
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * <p>
     * Download the remote file to the local computer. If the paths provided
     * are not absolute the current working directory is used.
     * </p>
     *
     * @param remote the path/name of the remote file
     * @param local the path/name to place the file on the local computer
     * @param progress
     *
     * @return
     *
     * @throws IOException if an IO error occurs or the file does not exist
     * @throws TransferCancelledException
     *
     * @since 0.2.0
     */
    public FileAttributes get(String remote, String local,
        FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        File localPath = resolveLocalPath(local);

        if (!localPath.exists()) {
            localPath.getParentFile().mkdirs();
            localPath.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(localPath);

        return get(remote, out, progress);
    }

    /**
     *
     *
     * @param remote
     * @param local
     *
     * @return
     *
     * @throws IOException
     */
    public FileAttributes get(String remote, String local)
        throws IOException {
        return get(remote, local, null);
    }

    /**
     * <p>
     * Download the remote file writing it to the specified
     * <code>OutputStream</code>. The OutputStream is closed by this mehtod
     * even if the operation fails.
     * </p>
     *
     * @param remote the path/name of the remote file
     * @param local the OutputStream to write
     * @param progress
     *
     * @return
     *
     * @throws IOException if an IO error occurs or the file does not exist
     * @throws TransferCancelledException
     *
     * @since 0.2.0
     */
    public FileAttributes get(String remote, OutputStream local,
        FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        String remotePath = resolveRemotePath(remote);
        FileAttributes attrs = stat(remotePath);

        if (progress != null) {
            progress.started(attrs.getSize().longValue(), remotePath);
        }

        SftpFileInputStream in = new SftpFileInputStream(sftp.openFile(
                    remotePath, SftpSubsystemClient.OPEN_READ));
        transferFile(in, local, progress);

        if (progress != null) {
            progress.completed();
        }

        return attrs;
    }

    /**
     *
     *
     * @param remote
     * @param local
     *
     * @return
     *
     * @throws IOException
     */
    public FileAttributes get(String remote, OutputStream local)
        throws IOException {
        return get(remote, local, null);
    }

    /**
     * <p>
     * Returns the state of the SFTP client. The client is closed if the
     * underlying session channel is closed. Invoking the <code>quit</code>
     * method of this object will close the underlying session channel.
     * </p>
     *
     * @return true if the client is still connected, otherwise false
     *
     * @since 0.2.0
     */
    public boolean isClosed() {
        return sftp.isClosed();
    }

    /**
     * <p>
     * Upload a file to the remote computer.
     * </p>
     *
     * @param local the path/name of the local file
     * @param progress
     *
     * @return
     *
     * @throws IOException if an IO error occurs or the file does not exist
     * @throws TransferCancelledException
     *
     * @since 0.2.0
     */
    public void put(String local, FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        File f = new File(local);
        put(local, f.getName(), progress);
    }

    /**
     *
     *
     * @param local
     *
     * @return
     *
     * @throws IOException
     */
    public void put(String local) throws IOException {
        put(local, (FileTransferProgress) null);
    }

    /**
     * <p>
     * Upload a file to the remote computer. If the paths provided are not
     * absolute the current working directory is used.
     * </p>
     *
     * @param local the path/name of the local file
     * @param remote the path/name of the destination file
     * @param progress
     *
     * @return
     *
     * @throws IOException if an IO error occurs or the file does not exist
     * @throws TransferCancelledException
     *
     * @since 0.2.0
     */
    public void put(String local, String remote, FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        File localPath = resolveLocalPath(local);
        FileInputStream in = new FileInputStream(localPath);

        try {
            FileAttributes attrs = stat(remote);

            if (attrs.isDirectory()) {
                File f = new File(local);
                remote += ((remote.endsWith("/") ? "" : "/") + f.getName());
            }
        } catch (IOException ex) {
        }

        put(in, remote, progress);
    }

    /**
     *
     *
     * @param local
     * @param remote
     *
     * @return
     *
     * @throws IOException
     */
    public void put(String local, String remote) throws IOException {
        put(local, remote, null);
    }

    /**
     * <p>
     * Upload a file to the remote computer reading from the specified <code>
     * InputStream</code>. The InputStream is closed, even if the operation
     * fails.
     * </p>
     *
     * @param in the InputStream being read
     * @param remote the path/name of the destination file
     * @param progress
     *
     * @return
     *
     * @throws IOException if an IO error occurs
     * @throws TransferCancelledException
     *
     * @since 0.2.0
     */
    public void put(InputStream in, String remote, FileTransferProgress progress)
        throws IOException, TransferCancelledException {
        String remotePath = resolveRemotePath(remote);
        SftpFileOutputStream out;
        FileAttributes attrs;
        boolean newfile = false;

        try {
            attrs = stat(remotePath);
            out = new SftpFileOutputStream(sftp.openFile(remotePath,
                        SftpSubsystemClient.OPEN_CREATE |
                        SftpSubsystemClient.OPEN_TRUNCATE |
                        SftpSubsystemClient.OPEN_WRITE));
        } catch (IOException ex) {
            attrs = new FileAttributes();
            newfile = true;
            attrs.setPermissions(new UnsignedInteger32(default_permissions ^
                    umask));
            out = new SftpFileOutputStream(sftp.openFile(remotePath,
                        SftpSubsystemClient.OPEN_CREATE |
                        SftpSubsystemClient.OPEN_WRITE, attrs));
        }

        if (progress != null) {
            progress.started(in.available(), remotePath);
        }

        transferFile(in, out, progress);

        if (progress != null) {
            progress.completed();
        }

        // Set the permissions here since at creation they dont always work
        if (newfile) {
            chmod(default_permissions ^ umask, remotePath);
        }
    }

    /**
     *
     *
     * @param in
     * @param remote
     *
     * @return
     *
     * @throws IOException
     */
    public void put(InputStream in, String remote) throws IOException {
        put(in, remote, null);
    }

    /**
     * <p>
     * Sets the user ID to owner for the file or directory.
     * </p>
     *
     * @param uid numeric user id of the new owner
     * @param path the path to the remote file/directory
     *
     * @throws IOException if an IO error occurs or the file does not exist
     *
     * @since 0.2.0
     */
    public void chown(int uid, String path) throws IOException {
        String actual = resolveRemotePath(path);
        FileAttributes attrs = sftp.getAttributes(actual);
        attrs.setUID(new UnsignedInteger32(uid));
        sftp.setAttributes(actual, attrs);
    }

    /**
     * <p>
     * Sets the group ID for the file or directory.
     * </p>
     *
     * @param gid the numeric group id for the new group
     * @param path the path to the remote file/directory
     *
     * @throws IOException if an IO error occurs or the file does not exist
     *
     * @since 0.2.0
     */
    public void chgrp(int gid, String path) throws IOException {
        String actual = resolveRemotePath(path);
        FileAttributes attrs = sftp.getAttributes(actual);
        attrs.setGID(new UnsignedInteger32(gid));
        sftp.setAttributes(actual, attrs);
    }

    /**
     * <p>
     * Changes the access permissions or modes of the specified file or
     * directory.
     * </p>
     *
     * <p>
     * Modes determine who can read, change or execute a file.
     * </p>
     * <blockquote><pre>Absolute modes are octal numbers specifying the complete list of
     * attributes for the files; you specify attributes by OR'ing together
     * these bits.
     *
     * 0400       Individual read
     * 0200       Individual write
     * 0100       Individual execute (or list directory)
     * 0040       Group read
     * 0020       Group write
     * 0010       Group execute
     * 0004       Other read
     * 0002       Other write
     * 0001       Other execute </pre></blockquote>
     *
     * @param permissions the absolute mode of the file/directory
     * @param path the path to the file/directory on the remote server
     *
     * @throws IOException if an IO error occurs or the file if not found
     *
     * @since 0.2.0
     */
    public void chmod(int permissions, String path) throws IOException {
        String actual = resolveRemotePath(path);
        sftp.changePermissions(actual, permissions);
    }

    public void umask(String umask) throws IOException {
        try {
            this.umask = Integer.parseInt(umask, 8);
        } catch (NumberFormatException ex) {
            throw new IOException(
                "umask must be 4 digit octal number e.g. 0022");
        }
    }

    /**
     * <p>
     * Rename a file on the remote computer.
     * </p>
     *
     * @param oldpath the old path
     * @param newpath the new path
     *
     * @throws IOException if an IO error occurs
     *
     * @since 0.2.0
     */
    public void rename(String oldpath, String newpath)
        throws IOException {
        String from = resolveRemotePath(oldpath);
        String to = resolveRemotePath(newpath);
        sftp.renameFile(from, to);
    }

    /**
     * <p>
     * Remove a file or directory from the remote computer.
     * </p>
     *
     * @param path the path of the remote file/directory
     *
     * @throws IOException if an IO error occurs
     *
     * @since 0.2.0
     */
    public void rm(String path) throws IOException {
        String actual = resolveRemotePath(path);
        FileAttributes attrs = sftp.getAttributes(actual);

        if (attrs.isDirectory()) {
            sftp.removeDirectory(actual);
        } else {
            sftp.removeFile(actual);
        }
    }

    /**
     *
     *
     * @param path
     * @param force
     * @param recurse
     *
     * @throws IOException
     */
    public void rm(String path, boolean force, boolean recurse)
        throws IOException {
        String actual = resolveRemotePath(path);
        FileAttributes attrs = sftp.getAttributes(actual);
        SftpFile file;

        if (attrs.isDirectory()) {
            List list = ls(path);

            if (!force && (list.size() > 0)) {
                throw new IOException(
                    "You cannot delete non-empty directory, use force=true to overide");
            } else {
                for (Iterator it = list.iterator(); it.hasNext();) {
                    file = (SftpFile) it.next();

                    if (file.isDirectory() && !file.getFilename().equals(".") &&
                            !file.getFilename().equals("..")) {
                        if (recurse) {
                            rm(file.getAbsolutePath(), force, recurse);
                        } else {
                            throw new IOException(
                                "Directory has contents, cannot delete without recurse=true");
                        }
                    } else if (file.isFile()) {
                        sftp.removeFile(file.getAbsolutePath());
                    }
                }
            }

            sftp.removeDirectory(actual);
        } else {
            sftp.removeFile(actual);
        }
    }

    /**
     * <p>
     * Create a symbolic link on the remote computer.
     * </p>
     *
     * @param path the path to the existing file
     * @param link the new link
     *
     * @throws IOException if an IO error occurs or the operation is not
     *         supported on the remote platform
     *
     * @since 0.2.0
     */
    public void symlink(String path, String link) throws IOException {
        String actualPath = resolveRemotePath(path);
        String actualLink = resolveRemotePath(link);
        sftp.createSymbolicLink(actualPath, actualLink);
    }

    /**
     * <p>
     * Returns the attributes of the file from the remote computer.
     * </p>
     *
     * @param path the path of the file on the remote computer
     *
     * @return the attributes
     *
     * @throws IOException if an IO error occurs or the file does not exist
     *
     * @see com.sshtools.j2ssh.sftp.FileAttributes
     * @since 0.2.0
     */
    public FileAttributes stat(String path) throws IOException {
        String actual = resolveRemotePath(path);

        return sftp.getAttributes(actual);
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
    public String getAbsolutePath(String path) throws IOException {
        String actual = resolveRemotePath(path);

        return sftp.getAbsolutePath(path);
    }

    /**
     * <p>
     * Close the SFTP client.
     * </p>
     *
     * @throws IOException
     *
     * @since 0.2.0
     */
    public void quit() throws IOException {
        sftp.close();
    }

    /**
     *
     *
     * @param localdir
     * @param remotedir
     * @param recurse
     * @param sync
     * @param commit
     * @param progress
     *
     * @return
     *
     * @throws IOException
     */
    public DirectoryOperation copyLocalDirectory(String localdir,
        String remotedir, boolean recurse, boolean sync, boolean commit,
        FileTransferProgress progress) throws IOException {
        DirectoryOperation op = new DirectoryOperation();

        // Record the previous
        String pwd = pwd();
        String lpwd = lpwd();
        File local = resolveLocalPath(localdir);
        remotedir = resolveRemotePath(remotedir);
        remotedir += (remotedir.endsWith("/") ? "" : "/");
        remotedir += local.getName();
        remotedir += (remotedir.endsWith("/") ? "" : "/");

        // Setup the remote directory if were committing
        if (commit) {
            try {
                FileAttributes attrs = stat(remotedir);
            } catch (IOException ex) {
                mkdir(remotedir);
            }
        }

        // List the local files and verify against the remote server
        File[] ls = local.listFiles();

        if (ls != null) {
            for (int i = 0; i < ls.length; i++) {
                if (ls[i].isDirectory() && !ls[i].getName().equals(".") &&
                        !ls[i].getName().equals("..")) {
                    if (recurse) {
                        File f = new File(local, ls[i].getName());
                        op.addDirectoryOperation(copyLocalDirectory(
                                f.getAbsolutePath(), remotedir, recurse, sync,
                                commit, progress), f);
                    }
                } else if (ls[i].isFile()) {
                    try {
                        FileAttributes attrs = stat(remotedir +
                                ls[i].getName());

                        if ((ls[i].length() == attrs.getSize().longValue()) &&
                                ((ls[i].lastModified() / 1000) == attrs.getModifiedTime()
                                                                           .longValue())) {
                            op.addUnchangedFile(ls[i]);
                        } else {
                            op.addUpdatedFile(ls[i]);
                        }
                    } catch (IOException ex1) {
                        op.addNewFile(ls[i]);
                    }

                    if (commit) {
                        put(ls[i].getAbsolutePath(),
                            remotedir + ls[i].getName(), progress);

                        FileAttributes attrs = stat(remotedir +
                                ls[i].getName());
                        attrs.setTimes(new UnsignedInteger32(
                                ls[i].lastModified() / 1000),
                            new UnsignedInteger32(ls[i].lastModified() / 1000));
                        sftp.setAttributes(remotedir + ls[i].getName(), attrs);
                    }
                }
            }
        }

        if (sync) {
            // List the contents of the new local directory and remove any
            // files/directories that were not updated
            try {
                List files = ls(remotedir);
                SftpFile file;
                File f;

                for (Iterator it = files.iterator(); it.hasNext();) {
                    file = (SftpFile) it.next();

                    // Create a local file object to test for its existence
                    f = new File(local, file.getFilename());

                    if (!op.containsFile(file) &&
                            !file.getFilename().equals(".") &&
                            !file.getFilename().equals("..")) {
                        op.addDeletedFile(file);

                        if (commit) {
                            if (file.isDirectory()) {
                                // Recurse through the directory, deleting stuff
                                recurseMarkForDeletion(file, op);

                                if (commit) {
                                    rm(file.getAbsolutePath(), true, true);
                                }
                            } else if (file.isFile()) {
                                rm(file.getAbsolutePath());
                            }
                        }
                    }
                }
            } catch (IOException ex2) {
                // Ignorew since if it does not exist we cant delete it
            }
        }

        // Return the operation details
        return op;
    }

    /**
     *
     *
     * @param eventListener
     */
    public void addEventListener(ChannelEventListener eventListener) {
        sftp.addEventListener(eventListener);
    }

    private void recurseMarkForDeletion(SftpFile file, DirectoryOperation op)
        throws IOException {
        List list = ls(file.getAbsolutePath());
        op.addDeletedFile(file);

        for (Iterator it = list.iterator(); it.hasNext();) {
            file = (SftpFile) it.next();

            if (file.isDirectory() && !file.getFilename().equals(".") &&
                    !file.getFilename().equals("..")) {
                recurseMarkForDeletion(file, op);
            } else if (file.isFile()) {
                op.addDeletedFile(file);
            }
        }
    }

    private void recurseMarkForDeletion(File file, DirectoryOperation op)
        throws IOException {
        File[] list = file.listFiles();
        op.addDeletedFile(file);

        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                file = list[i];

                if (file.isDirectory() && !file.getName().equals(".") &&
                        !file.getName().equals("..")) {
                    recurseMarkForDeletion(file, op);
                } else if (file.isFile()) {
                    op.addDeletedFile(file);
                }
            }
        }
    }

    /**
     *
     *
     * @param remotedir
     * @param localdir
     * @param recurse
     * @param sync
     * @param commit
     * @param progress
     *
     * @return
     *
     * @throws IOException
     */
    public DirectoryOperation copyRemoteDirectory(String remotedir,
        String localdir, boolean recurse, boolean sync, boolean commit,
        FileTransferProgress progress) throws IOException {
        // Create an operation object to hold the information
        DirectoryOperation op = new DirectoryOperation();

        // Record the previous working directoies
        String pwd = pwd();
        String lpwd = lpwd();
        cd(remotedir);

        // Setup the local cwd
        String base = remotedir;
        int idx = base.lastIndexOf('/');

        if (idx != -1) {
            base = base.substring(idx + 1);
        }

        File local = new File(localdir, base);

        //				File local = new File(localdir, remotedir);
        if (!local.isAbsolute()) {
            local = new File(lpwd(), localdir);
        }

        if (!local.exists() && commit) {
            local.mkdir();
        }

        List files = ls();
        SftpFile file;
        File f;

        for (Iterator it = files.iterator(); it.hasNext();) {
            file = (SftpFile) it.next();

            if (file.isDirectory() && !file.getFilename().equals(".") &&
                    !file.getFilename().equals("..")) {
                if (recurse) {
                    f = new File(local, file.getFilename());
                    op.addDirectoryOperation(copyRemoteDirectory(
                            file.getFilename(), local.getAbsolutePath(),
                            recurse, sync, commit, progress), f);
                }
            } else if (file.isFile()) {
                f = new File(local, file.getFilename());

                if (f.exists() &&
                        (f.length() == file.getAttributes().getSize().longValue()) &&
                        ((f.lastModified() / 1000) == file.getAttributes()
                                                              .getModifiedTime()
                                                              .longValue())) {
                    if (commit) {
                        op.addUnchangedFile(f);
                    } else {
                        op.addUnchangedFile(file);
                    }

                    continue;
                }

                if (f.exists()) {
                    if (commit) {
                        op.addUpdatedFile(f);
                    } else {
                        op.addUpdatedFile(file);
                    }
                } else {
                    if (commit) {
                        op.addNewFile(f);
                    } else {
                        op.addNewFile(file);
                    }
                }

                if (commit) {
                    FileAttributes attrs = get(file.getFilename(),
                            f.getAbsolutePath(), progress);
                    f.setLastModified(attrs.getModifiedTime().longValue() * 1000);
                }
            }
        }

        if (sync) {
            // List the contents of the new local directory and remove any
            // files/directories that were not updated
            File[] contents = local.listFiles();

            if (contents != null) {
                for (int i = 0; i < contents.length; i++) {
                    if (!op.containsFile(contents[i])) {
                        op.addDeletedFile(contents[i]);

                        if (contents[i].isDirectory() &&
                                !contents[i].getName().equals(".") &&
                                !contents[i].getName().equals("..")) {
                            recurseMarkForDeletion(contents[i], op);

                            if (commit) {
                                IOUtil.recurseDeleteDirectory(contents[i]);
                            }
                        } else if (commit) {
                            contents[i].delete();
                        }
                    }
                }
            }
        }

        cd(pwd);

        return op;
    }
}
