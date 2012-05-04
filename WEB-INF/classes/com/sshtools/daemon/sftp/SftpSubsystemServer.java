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
package com.sshtools.daemon.sftp;

import com.sshtools.daemon.platform.*;
import com.sshtools.daemon.session.*;
import com.sshtools.daemon.subsystem.*;

import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.connection.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.sftp.*;
import com.sshtools.j2ssh.subsystem.*;

import org.apache.commons.logging.*;

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class SftpSubsystemServer extends SubsystemServer {
    /**  */
    public static final int VERSION_1 = 1;

    /**  */
    public static final int VERSION_2 = 2;

    /**  */
    public static final int VERSION_3 = 3;

    /**  */
    public static final int VERSION_4 = 4;
    private static Log log = LogFactory.getLog(SftpSubsystemServer.class);
    private NativeFileSystemProvider nfs;

    /**
 * Creates a new SftpSubsystemServer object.
 */
    public SftpSubsystemServer() {
        registerMessage(SshFxpInit.SSH_FXP_INIT, SshFxpInit.class);
        registerMessage(SshFxpMkdir.SSH_FXP_MKDIR, SshFxpMkdir.class);
        registerMessage(SshFxpRealPath.SSH_FXP_REALPATH, SshFxpRealPath.class);
        registerMessage(SshFxpOpenDir.SSH_FXP_OPENDIR, SshFxpOpenDir.class);
        registerMessage(SshFxpOpen.SSH_FXP_OPEN, SshFxpOpen.class);
        registerMessage(SshFxpRead.SSH_FXP_READ, SshFxpRead.class);
        registerMessage(SshFxpWrite.SSH_FXP_WRITE, SshFxpWrite.class);
        registerMessage(SshFxpReadDir.SSH_FXP_READDIR, SshFxpReadDir.class);
        registerMessage(SshFxpClose.SSH_FXP_CLOSE, SshFxpClose.class);
        registerMessage(SshFxpLStat.SSH_FXP_LSTAT, SshFxpLStat.class);
        registerMessage(SshFxpStat.SSH_FXP_STAT, SshFxpStat.class);
        registerMessage(SshFxpRemove.SSH_FXP_REMOVE, SshFxpRemove.class);
        registerMessage(SshFxpRename.SSH_FXP_RENAME, SshFxpRename.class);
        registerMessage(SshFxpRmdir.SSH_FXP_RMDIR, SshFxpRmdir.class);
        registerMessage(SshFxpSetStat.SSH_FXP_SETSTAT, SshFxpSetStat.class);
        registerMessage(SshFxpFStat.SSH_FXP_FSTAT, SshFxpFStat.class);
        registerMessage(SshFxpFSetStat.SSH_FXP_FSETSTAT, SshFxpFSetStat.class);
        registerMessage(SshFxpReadlink.SSH_FXP_READLINK, SshFxpReadlink.class);
        registerMessage(SshFxpSymlink.SSH_FXP_SYMLINK, SshFxpSymlink.class);
    }

    /**
 *
 *
 * @param session
 */
    public void setSession(SessionChannelServer session) {
        session.addEventListener(new ChannelEventListener() {
                public void onChannelOpen(Channel channel) {
                }

                public void onChannelEOF(Channel channel) {
                    try {
                        SftpSubsystemServer.this.session.close();
                    } catch (IOException ex) {
                    }
                }

                public void onChannelClose(Channel channel) {
                }

                public void onDataReceived(Channel channel, byte[] data) {
                }

                public void onDataSent(Channel channel, byte[] data) {
                }
            });
        super.setSession(session);
    }

    /**
 *
 *
 * @param msg
 */
    protected void onMessageReceived(SubsystemMessage msg) {
        switch (msg.getMessageType()) {
        case SshFxpInit.SSH_FXP_INIT: {
            onInitialize((SshFxpInit) msg);

            break;
        }

        case SshFxpMkdir.SSH_FXP_MKDIR: {
            onMakeDirectory((SshFxpMkdir) msg);

            break;
        }

        case SshFxpRealPath.SSH_FXP_REALPATH: {
            onRealPath((SshFxpRealPath) msg);

            break;
        }

        case SshFxpOpenDir.SSH_FXP_OPENDIR: {
            onOpenDirectory((SshFxpOpenDir) msg);

            break;
        }

        case SshFxpOpen.SSH_FXP_OPEN: {
            onOpenFile((SshFxpOpen) msg);

            break;
        }

        case SshFxpRead.SSH_FXP_READ: {
            onReadFile((SshFxpRead) msg);

            break;
        }

        case SshFxpWrite.SSH_FXP_WRITE: {
            onWriteFile((SshFxpWrite) msg);

            break;
        }

        case SshFxpReadDir.SSH_FXP_READDIR: {
            onReadDirectory((SshFxpReadDir) msg);

            break;
        }

        case SshFxpLStat.SSH_FXP_LSTAT: {
            onLStat((SshFxpLStat) msg);

            break;
        }

        case SshFxpStat.SSH_FXP_STAT: {
            onStat((SshFxpStat) msg);

            break;
        }

        case SshFxpFStat.SSH_FXP_FSTAT: {
            onFStat((SshFxpFStat) msg);

            break;
        }

        case SshFxpClose.SSH_FXP_CLOSE: {
            onCloseFile((SshFxpClose) msg);

            break;
        }

        case SshFxpRemove.SSH_FXP_REMOVE: {
            onRemoveFile((SshFxpRemove) msg);

            break;
        }

        case SshFxpRename.SSH_FXP_RENAME: {
            onRenameFile((SshFxpRename) msg);

            break;
        }

        case SshFxpRmdir.SSH_FXP_RMDIR: {
            onRemoveDirectory((SshFxpRmdir) msg);

            break;
        }

        case SshFxpSetStat.SSH_FXP_SETSTAT: {
            onSetAttributes((SshFxpSetStat) msg);

            break;
        }

        case SshFxpFSetStat.SSH_FXP_FSETSTAT: {
            onSetAttributes((SshFxpFSetStat) msg);

            break;
        }

        case SshFxpReadlink.SSH_FXP_READLINK: {
            onReadlink((SshFxpReadlink) msg);

            break;
        }

        case SshFxpSymlink.SSH_FXP_SYMLINK: {
            onSymlink((SshFxpSymlink) msg);

            break;
        }

        default: {
        }
        }
    }

    private void onSetAttributes(SshFxpSetStat msg) {
        SubsystemMessage reply;

        try {
            nfs.setFileAttributes(checkDefaultPath(msg.getPath()),
                msg.getAttributes());
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The attributes were set", "");
        } catch (FileNotFoundException fnfe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    fnfe.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        } catch (IOException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onSetAttributes(SshFxpFSetStat msg) {
        SubsystemMessage reply;

        try {
            nfs.setFileAttributes(msg.getHandle(), msg.getAttributes());
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The attributes were set", "");
        } catch (InvalidHandleException ihe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ihe.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        } catch (IOException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onReadlink(SshFxpReadlink msg) {
        SubsystemMessage reply;

        try {
            /*
     File f = nfs.readSymbolicLink(VirtualFileSystem.translateVFSPath(
        msg.getPath()));
             SftpFile[] files = new SftpFile[1];
             files[0] = new SftpFile(VirtualFileSystem.translateNFSPath(
        f.getCanonicalPath()),
    nfs.getFileAttributes(f.getCanonicalPath()));
             reply = new SshFxpName(msg.getId(), files);
 */
            reply = new SshFxpName(msg.getId(),
                    new SftpFile[] {
                        nfs.readSymbolicLink(checkDefaultPath(msg.getPath()))
                    });
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        } catch (UnsupportedFileOperationException uso) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OP_UNSUPPORTED),
                    uso.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onSymlink(SshFxpSymlink msg) {
        SubsystemMessage reply;

        try {
            /*
             nfs.createSymbolicLink(VirtualFileSystem.translateVFSPath(
    msg.getLinkPath()),
             VirtualFileSystem.translateVFSPath(msg.getTargetPath()));
 */
            nfs.createSymbolicLink(checkDefaultPath(msg.getLinkPath()),
                checkDefaultPath(msg.getTargetPath()));
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The symbolic link was created", "");
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        } catch (UnsupportedFileOperationException uso) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OP_UNSUPPORTED),
                    uso.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onRemoveDirectory(SshFxpRmdir msg) {
        SubsystemMessage reply;

        try {
            /*
             nfs.removeDirectory(VirtualFileSystem.translateVFSPath(
    msg.getPath()));
 */
            nfs.removeDirectory(checkDefaultPath(msg.getPath()));
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The directory was removed", "");
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onRenameFile(SshFxpRename msg) {
        SubsystemMessage reply;

        try {
            /*
     nfs.renameFile(VirtualFileSystem.translateVFSPath(msg.getOldPath()),
             VirtualFileSystem.translateVFSPath(msg.getNewPath()));
 */
            nfs.renameFile(checkDefaultPath(msg.getOldPath()),
                checkDefaultPath(msg.getNewPath()));
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The file was removed", "");
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onRemoveFile(SshFxpRemove msg) {
        SubsystemMessage reply;

        try {
            /*
     nfs.removeFile(VirtualFileSystem.translateVFSPath(msg.getFilename()));
 */
            nfs.removeFile(checkDefaultPath(msg.getFilename()));
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The file was removed", "");
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onOpenFile(SshFxpOpen msg) {
        SubsystemMessage reply;

        try {
            reply = new SshFxpHandle(msg.getId(),
                    nfs.openFile(checkDefaultPath(msg.getFilename()),
                        msg.getPflags(), msg.getAttributes()));
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onReadFile(SshFxpRead msg) {
        SubsystemMessage reply;

        try {
            reply = new SshFxpData(msg.getId(),
                    nfs.readFile(msg.getHandle(), msg.getOffset(),
                        msg.getLength()));
        } catch (EOFException eof) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_EOF),
                    eof.getMessage(), "");
        } catch (InvalidHandleException ihe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ihe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onWriteFile(SshFxpWrite msg) {
        SubsystemMessage reply;

        try {
            nfs.writeFile(msg.getHandle(), msg.getOffset(), msg.getData(), 0,
                msg.getData().length);
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The write completed successfully", "");
        } catch (InvalidHandleException ihe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ihe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onCloseFile(SshFxpClose msg) {
        SubsystemMessage reply;

        try {
            nfs.closeFile(msg.getHandle());
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                    "The operation completed", "");
        } catch (InvalidHandleException ihe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ihe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onFStat(SshFxpFStat msg) {
        SubsystemMessage reply;

        try {
            reply = new SshFxpAttrs(msg.getId(),
                    nfs.getFileAttributes(msg.getHandle()));
        } catch (InvalidHandleException ihe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ihe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onStat(SshFxpStat msg) {
        SubsystemMessage reply;

        try {
            String path = checkDefaultPath(msg.getPath());

            if (nfs.fileExists(path)) {
                SftpFile[] files = new SftpFile[1];
                reply = new SshFxpAttrs(msg.getId(),
                        nfs.getFileAttributes(
                    /*nfs.getCanonicalPath(*/
                    msg.getPath() /*)*/));
            } else {
                reply = new SshFxpStatus(msg.getId(),
                        new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                        path + " is not a valid file path", "");
            }
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onLStat(SshFxpLStat msg) {
        SubsystemMessage reply;

        try {
            String path = checkDefaultPath(msg.getPath());

            if (nfs.fileExists(path)) {
                SftpFile[] files = new SftpFile[1];
                reply = new SshFxpAttrs(msg.getId(),
                        nfs.getFileAttributes(nfs.getCanonicalPath(
                                msg.getPath())));
            } else {
                reply = new SshFxpStatus(msg.getId(),
                        new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                        path + " is not a valid file path", "");
            }
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onReadDirectory(SshFxpReadDir msg) {
        SubsystemMessage reply;

        try {
            /*
             File[] files = nfs.readDirectory(msg.getHandle());
             SftpFile[] sftpfiles = new SftpFile[files.length];
             for (int i = 0; i < files.length; i++) {
             sftpfiles[i] = new SftpFile(files[i].getName(),
        nfs.getFileAttributes(files[i].getCanonicalPath()));
             }
 */
            SftpFile[] sftpfiles = nfs.readDirectory(msg.getHandle());
            reply = new SshFxpName(msg.getId(), sftpfiles);
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (InvalidHandleException ihe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ihe.getMessage(), "");
        } catch (EOFException eof) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_EOF),
                    eof.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onOpenDirectory(SshFxpOpenDir msg) {
        SubsystemMessage reply;

        try {
            /*
     String path = VirtualFileSystem.translateVFSPath(msg.getPath());
 */
            String path = checkDefaultPath(msg.getPath());
            reply = new SshFxpHandle(msg.getId(), nfs.openDirectory(path));
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe2) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe2.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onRealPath(SshFxpRealPath msg) {
        SubsystemMessage reply;

        try {
            /*
     String path = VirtualFileSystem.translateVFSPath(msg.getPath());
             path = VirtualFileSystem.translateNFSPath(path);
 */
            String path = nfs.getRealPath(checkDefaultPath(msg.getPath()));

            if (path != null) {
                SftpFile[] files = new SftpFile[1];
                files[0] = new SftpFile(path);
                reply = new SshFxpName(msg.getId(), files);
            } else {
                reply = new SshFxpStatus(msg.getId(),
                        new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                        msg.getPath() +
                        " could not be translated into a system dependent path",
                        "");
            }
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (IOException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe.getMessage(), "");
        }

        sendMessage(reply);
    }

    private void onMakeDirectory(SshFxpMkdir msg) {
        SubsystemMessage reply;

        try {
            /*
     String path = VirtualFileSystem.translateVFSPath(msg.getPath());
 */
            String path = checkDefaultPath(msg.getPath());

            if (nfs.makeDirectory(path)) {
                reply = new SshFxpStatus(msg.getId(),
                        new UnsignedInteger32(SshFxpStatus.STATUS_FX_OK),
                        "The operation completed sucessfully", "");
            } else {
                // Send an error back to the client
                reply = new SshFxpStatus(msg.getId(),
                        new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                        "The operation failed", "");
            }
        } catch (FileNotFoundException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_NO_SUCH_FILE),
                    ioe.getMessage(), "");
        } catch (PermissionDeniedException pde) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_PERMISSION_DENIED),
                    pde.getMessage(), "");
        } catch (IOException ioe) {
            reply = new SshFxpStatus(msg.getId(),
                    new UnsignedInteger32(SshFxpStatus.STATUS_FX_FAILURE),
                    ioe.getMessage(), "");
        }

        sendMessage(reply);
    }

    private String checkDefaultPath(String path) throws IOException {
        // Use the users home directory if no path is supplied
        if (path.equals("")) {
            return nfs.getDefaultPath(SshThread.getCurrentThreadUser());
        } else {
            return path;
        }
    }

    private void onInitialize(SshFxpInit msg) {
        // Get the native file system
        nfs = NativeFileSystemProvider.getInstance();

        // Determine the users home directory
        if (msg.getVersion().intValue() == VERSION_3) {
            SshFxpVersion reply = new SshFxpVersion(new UnsignedInteger32(
                        VERSION_3), null);
            sendMessage(reply);
        } else {
            // Wrong version
        }
    }
}
