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
package com.sshtools.daemon.scp;

import com.sshtools.daemon.platform.*;
import com.sshtools.daemon.util.*;

import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.sftp.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.8 $
 */
public class ScpServer extends NativeProcessProvider implements Runnable {
    private static Log log = LogFactory.getLog(ScpServer.class);
    private static int BUFFER_SIZE = 16384;

    //	Private instance variables
    private InputStream in;

    //	Private instance variables
    private InputStream err;
    private OutputStream out;
    private String destination;
    private PipedOutputStream pipeIn;
    private PipedOutputStream pipeErr;
    private PipedInputStream pipeOut;
    private SshThread scpServerThread;
    private int verbosity = 0;
    private int exitCode;
    private boolean directory;
    private boolean recursive;
    private boolean from;
    private boolean to;
    private NativeFileSystemProvider nfs;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private String currentDirectory;
    private boolean preserveAttributes;

    /**
 * Creates a new ScpServer object.
 */
    public ScpServer() {
        nfs = NativeFileSystemProvider.getInstance();
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#allocatePseudoTerminal(java.lang.String, int, int, int, int, java.lang.String)
 */
    public boolean allocatePseudoTerminal(String term, int cols, int rows,
        int width, int height, String modes) {
        return false;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#createProcess(java.lang.String, java.util.Map)
 */
    public boolean createProcess(String command, Map environment)
        throws IOException {
        log.info("Creating ScpServer");

        if (nfs == null) {
            throw new IOException(
                "NativeFileSystem was not instantiated. Please check logs");
        }

        scp(command.substring(4));

        return true;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#getDefaultTerminalProvider()
 */
    public String getDefaultTerminalProvider() {
        return null;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#getInputStream()
 */
    public InputStream getInputStream() throws IOException {
        return in;
    }

    /* (non-Javadoc)
     * @see com.sshtools.daemon.platform.NativeProcessProvider#getStderrInputStream()
 */
    public InputStream getStderrInputStream() {
        return err;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#getOutputStream()
 */
    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#kill()
 */
    public void kill() {
        log.info("Killing ScpServer");

        try {
            if (pipeIn != null) {
                pipeIn.close();
            }
        } catch (IOException ioe) {
        }

        try {
            if (pipeOut != null) {
                pipeOut.close();
            }
        } catch (IOException ioe) {
        }

        try {
            if (pipeErr != null) {
                pipeErr.close();
            }
        } catch (IOException ioe) {
        }
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#start()
 */
    public void start() throws IOException {
        log.debug("Starting ScpServer thread");
        scpServerThread = SshThread.getCurrentThread().cloneThread(this,
                "ScpServer");
        scpServerThread.start();
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#stillActive()
 */
    public boolean stillActive() {
        return false;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#supportsPseudoTerminal(java.lang.String)
 */
    public boolean supportsPseudoTerminal(String term) {
        return false;
    }

    /* (non-Javadoc)
 * @see com.sshtools.daemon.platform.NativeProcessProvider#waitForExitCode()
 */
    public int waitForExitCode() {
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException ie) {
        }

        log.debug("Returning exit code of " + exitCode);

        return exitCode;
    }

    private void scp(String args) throws IOException {
        log.debug("Parsing ScpServer options " + args);

        //	Parse the command line for supported options
        String[] a = StringUtil.current().allParts(args, " ");
        destination = null;
        directory = false;
        from = false;
        to = false;
        recursive = false;
        verbosity = 0;

        boolean remote = false;

        for (int i = 0; i < a.length; i++) {
            if (a[i].startsWith("-")) {
                String s = a[i].substring(1);

                for (int j = 0; j < s.length(); j++) {
                    char ch = s.charAt(j);

                    switch (ch) {
                    case 't':
                        to = true;

                        break;

                    case 'd':
                        directory = true;

                        break;

                    case 'f':
                        from = true;

                        break;

                    case 'r':
                        recursive = true;

                        break;

                    case 'v':
                        verbosity++;

                        break;

                    case 'p':
                        preserveAttributes = true;

                        break;

                    default:
                        log.warn("Unsupported argument, allowing to continue.");
                    }
                }
            } else {
                if (destination == null) {
                    destination = a[i];
                } else {
                    throw new IOException("More than one destination supplied " +
                        a[i]);
                }
            }
        }

        if (!to && !from) {
            throw new IOException("Must supply either -t or -f.");
        }

        if (destination == null) {
            throw new IOException("Destination not supplied.");
        }

        log.debug("Destination is " + destination);
        log.debug("Recursive is " + recursive);
        log.debug("Directory is " + directory);
        log.debug("Verbosity is " + verbosity);
        log.debug("From is " + from);
        log.debug("To is " + to);
        log.debug("Preserve Attributes " + preserveAttributes);

        //	Start the SCP server
        log.debug("Creating pipes");
        pipeIn = new PipedOutputStream();
        pipeErr = new PipedOutputStream();
        pipeOut = new PipedInputStream();
        in = new PipedInputStream(pipeIn);
        err = new PipedInputStream(pipeErr);
        out = new PipedOutputStream(pipeOut);
    }

    /**
 * Send ok command to client
 *
 * @throws IOException on any error
 */
    private void writeOk() throws IOException {
        log.debug("Sending client ok command");
        pipeIn.write(0);
        pipeIn.flush();
    }

    /**
 * Send command to client
 *
 * @param cmd command
 *
 * @throws IOException on any error
 */
    private void writeCommand(String cmd) throws IOException {
        log.debug("Sending command '" + cmd + "'");
        pipeIn.write(cmd.getBytes());

        if (!cmd.endsWith("\n")) {
            pipeIn.write("\n".getBytes());
        }

        pipeIn.flush();
    }

    /**
 * Send error message to client
 *
 * @param msg error message
 *
 * @throws IOException on any error
 */
    private void writeError(String msg) throws IOException {
        writeError(msg, false);
    }

    /**
 * Send error message to client
 *
 * @param msg error message
 * @param serious serious error
 *
 * @throws IOException on any error
 */
    private void writeError(String msg, boolean serious)
        throws IOException {
        log.debug("Sending error message '" + msg + "' to client (serious=" +
            serious + ")");
        pipeIn.write(serious ? 2 : 1);
        pipeIn.write(msg.getBytes());

        if (!msg.endsWith("\n")) {
            pipeIn.write('\n');
        }

        pipeIn.flush();
    }

    /* (non-Javadoc)
 * @see java.lang.Runnable#run()
 */
    public void run() {
        log.debug("Running ScpServer thread");

        try {
            if (from) {
                log.info("From mode");

                try {
                    waitForResponse();

                    //	Build a string pattern that may be used to match wildcards
                    StringPattern sp = new StringPattern(destination);

                    /*If this looks like a wildcard, then attempt a simple expansion.
 * This only work for the base part of the file name at the moment
 */
                    if (sp.hasWildcard()) {
                        log.debug("Path contains wildcard");

                        String base = destination;
                        String dir = ".";
                        int idx = base.lastIndexOf('/');

                        if (idx != -1) {
                            if (idx > 0) {
                                dir = base.substring(0, idx);
                            }

                            base = base.substring(idx + 1);
                        }

                        log.debug("Looking for matches in " + dir + " for " +
                            base);
                        sp = new StringPattern(base);

                        byte[] handle = null;

                        try {
                            handle = nfs.openDirectory(dir);

                            SftpFile[] files = nfs.readDirectory(handle);

                            for (int i = 0; i < files.length; i++) {
                                log.debug("Testing for match against " +
                                    files[i].getFilename());

                                if (sp.matches(files[i].getFilename())) {
                                    log.debug("Matched");
                                    writeFileToRemote(dir + "/" +
                                        files[i].getFilename());
                                } else {
                                    log.debug("No match");
                                }
                            }
                        } finally {
                            if (handle != null) {
                                try {
                                    nfs.closeFile(handle);
                                } catch (Exception e) {
                                }
                            }
                        }
                    } else {
                        log.debug("No wildcards");
                        writeFileToRemote(destination);
                    }

                    log.debug("File transfers complete");
                } catch (FileNotFoundException fnfe) {
                    log.error(fnfe);
                    writeError(fnfe.getMessage(), true);
                    throw new IOException(fnfe.getMessage());
                } catch (PermissionDeniedException pde) {
                    log.error(pde);
                    writeError(pde.getMessage(), true);
                    throw new IOException(pde.getMessage());
                } catch (InvalidHandleException ihe) {
                    log.error(ihe);
                    writeError(ihe.getMessage(), true);
                    throw new IOException(ihe.getMessage());
                } catch (IOException ioe) {
                    log.error(ioe);
                    writeError(ioe.getMessage(), true);
                    throw new IOException(ioe.getMessage());
                }
            } else {
                log.info("To mode");
                readFromRemote(destination);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            log.error(t);
            exitCode = 1;
        }

        //
        log.debug("ScpServer stopped, notify block on waitForExitCode().");

        synchronized (this) {
            notify();
        }
    }

    private boolean writeDirToRemote(String path) throws IOException {
        FileAttributes attr = nfs.getFileAttributes(path);

        if (attr.isDirectory() && !recursive) {
            writeError("File " + path + " is a directory, use recursive mode");

            return false;
        }

        String basename = path;
        int idx = path.lastIndexOf('/');

        if (idx != -1) {
            basename = path.substring(idx + 1);
        }

        writeCommand("D" + attr.getMaskString() + " 0 " + basename + "\n");
        waitForResponse();

        byte[] handle = null;

        try {
            handle = nfs.openDirectory(path);

            SftpFile[] list = nfs.readDirectory(handle);

            for (int i = 0; i < list.length; i++) {
                writeFileToRemote(path + "/" + list[i].getFilename());
            }

            writeCommand("E");
        } catch (InvalidHandleException ihe) {
            throw new IOException(ihe.getMessage());
        } catch (PermissionDeniedException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (handle != null) {
                try {
                    nfs.closeFile(handle);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }

        return true;
    }

    private void writeFileToRemote(String path)
        throws IOException, PermissionDeniedException, InvalidHandleException {
        FileAttributes attr = nfs.getFileAttributes(path);

        if (attr.isDirectory()) {
            if (!writeDirToRemote(path)) {
                return;
            }
        } else if (attr.isFile()) {
            String basename = path;
            int idx = basename.lastIndexOf('/');

            if (idx != -1) {
                basename = path.substring(idx + 1);
            }

            // TODO: Deal with permissions properly
            writeCommand("C" + attr.getMaskString() + " " + attr.getSize() +
                " " + basename + "\n");
            waitForResponse();
            log.debug("Opening file " + path);

            byte[] handle = null;

            try {
                handle = nfs.openFile(path,
                        new UnsignedInteger32(
                            NativeFileSystemProvider.OPEN_READ), attr);

                int count = 0;
                log.debug("Sending file");

                while (count < attr.getSize().intValue()) {
                    try {
                        byte[] buf = nfs.readFile(handle,
                                new UnsignedInteger64(String.valueOf(count)),
                                new UnsignedInteger32(BUFFER_SIZE));
                        count += buf.length;
                        log.debug("Writing block of " + buf.length + " bytes");
                        pipeIn.write(buf);
                    } catch (EOFException eofe) {
                        log.debug("End of file - finishing transfer");

                        break;
                    }
                }

                pipeIn.flush();

                if (count < attr.getSize().intValue()) {
                    throw new IOException(
                        "File transfer terminated abnormally.");
                } else {
                    log.info("File transfer complete.");
                }

                writeOk();
            } finally {
                if (handle != null) {
                    try {
                        nfs.closeFile(handle);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        } else {
            throw new IOException(path + " not valid for SCP.");
        }

        waitForResponse();
    }

    private void waitForResponse() throws IOException {
        log.debug("Waiting for response");

        int r = pipeOut.read();

        if (r == 0) {
            log.debug("Got Ok");

            // All is well, no error
            return;
        }

        if (r == -1) {
            throw new EOFException("SCP returned unexpected EOF");
        }

        String msg = readString();
        log.debug("Got error '" + msg + "'");

        if (r == (byte) '\02') {
            log.debug("This is a serious error");
            throw new IOException(msg);
        }

        throw new IOException("SCP returned an unexpected error: " + msg);
    }

    private void readFromRemote(String path) throws IOException {
        String cmd;
        String[] cmdParts = new String[3];
        writeOk();

        while (true) {
            log.debug("Waiting for command");

            try {
                cmd = readString();
            } catch (EOFException e) {
                return;
            }

            log.debug("Got command '" + cmd + "'");

            char cmdChar = cmd.charAt(0);

            switch (cmdChar) {
            case 'E':
                writeOk();

                return;

            case 'T':
                log.error("SCP time not currently supported");
                writeError(
                    "WARNING: This server does not currently support the SCP time command");

                break;

            case 'C':
            case 'D':
                parseCommand(cmd, cmdParts);

                FileAttributes attr = null;

                try {
                    log.debug("Getting attributes for current destination (" +
                        path + ")");
                    attr = nfs.getFileAttributes(path);
                } catch (FileNotFoundException fnfe) {
                    log.debug("Current destination not found");
                }

                String targetPath = path;
                String name = cmdParts[2];

                if ((attr != null) && attr.isDirectory()) {
                    log.debug("Target is a directory");
                    targetPath += ('/' + name);
                }

                FileAttributes targetAttr = null;

                try {
                    log.debug("Getting attributes for target destination (" +
                        targetPath + ")");
                    targetAttr = nfs.getFileAttributes(targetPath);
                } catch (FileNotFoundException fnfe) {
                    log.debug("Target destination not found");
                }

                if (cmdChar == 'D') {
                    log.debug("Got directory request");

                    if (targetAttr != null) {
                        if (!targetAttr.isDirectory()) {
                            String msg = "Invalid target " + name +
                                ", must be a directory";
                            writeError(msg);
                            throw new IOException(msg);
                        }
                    } else {
                        try {
                            log.debug("Creating directory " + targetPath);

                            if (!nfs.makeDirectory(targetPath)) {
                                String msg = "Could not create directory: " +
                                    name;
                                writeError(msg);
                                throw new IOException(msg);
                            } else {
                                log.debug("Setting permissions on directory");
                                attr.setPermissionsFromMaskString(cmdParts[0]);
                            }
                        } catch (FileNotFoundException e1) {
                            writeError("File not found");
                            throw new IOException("File not found");
                        } catch (PermissionDeniedException e1) {
                            writeError("Permission denied");
                            throw new IOException("Permission denied");
                        }
                    }

                    readFromRemote(targetPath);

                    continue;
                }

                log.debug("Opening file for writing");

                byte[] handle = null;

                try {
                    // Open the file
                    handle = nfs.openFile(targetPath,
                            new UnsignedInteger32(NativeFileSystemProvider.OPEN_CREATE |
                                NativeFileSystemProvider.OPEN_WRITE |
                                NativeFileSystemProvider.OPEN_TRUNCATE), attr);
                    log.debug("NFS file opened");
                    writeOk();
                    log.debug("Reading from client");

                    int count = 0;
                    int read;
                    long length = Long.parseLong(cmdParts[1]);

                    while (count < length) {
                        read = pipeOut.read(buffer, 0,
                                (int) (((length - count) < buffer.length)
                                ? (length - count) : buffer.length));

                        if (read == -1) {
                            throw new EOFException(
                                "ScpServer received an unexpected EOF during file transfer");
                        }

                        log.debug("Got block of " + read);
                        nfs.writeFile(handle,
                            new UnsignedInteger64(String.valueOf(count)),
                            buffer, 0, read);
                        count += read;
                    }

                    log.debug("File transfer complete");
                } catch (InvalidHandleException ihe) {
                    writeError("Invalid handle.");
                    throw new IOException("Invalid handle.");
                } catch (FileNotFoundException e) {
                    writeError("File not found");
                    throw new IOException("File not found");
                } catch (PermissionDeniedException e) {
                    writeError("Permission denied");
                    throw new IOException("Permission denied");
                } finally {
                    if (handle != null) {
                        try {
                            log.debug("Closing handle");
                            nfs.closeFile(handle);
                        } catch (Exception e) {
                        }
                    }
                }

                waitForResponse();

                if (preserveAttributes) {
                    attr.setPermissionsFromMaskString(cmdParts[0]);
                    log.debug("Setting permissions on directory to " +
                        attr.getPermissionsString());

                    try {
                        nfs.setFileAttributes(targetPath, attr);
                    } catch (Exception e) {
                        writeError("Failed to set file permissions.");

                        break;
                    }
                }

                writeOk();

                break;

            default:
                writeError("Unexpected cmd: " + cmd);
                throw new IOException("SCP unexpected cmd: " + cmd);
            }
        }
    }

    private void parseCommand(String cmd, String[] cmdParts)
        throws IOException {
        int l;
        int r;
        l = cmd.indexOf(' ');
        r = cmd.indexOf(' ', l + 1);

        if ((l == -1) || (r == -1)) {
            writeError("Syntax error in cmd");
            throw new IOException("Syntax error in cmd");
        }

        cmdParts[0] = cmd.substring(1, l);
        cmdParts[1] = cmd.substring(l + 1, r);
        cmdParts[2] = cmd.substring(r + 1);
    }

    private String readString() throws IOException {
        int ch;
        int i = 0;

        while (((ch = pipeOut.read()) != ((int) '\n')) && (ch >= 0)) {
            buffer[i++] = (byte) ch;
        }

        if (ch == -1) {
            throw new EOFException("SCP returned unexpected EOF");
        }

        if (buffer[0] == (byte) '\n') {
            throw new IOException("Unexpected <NL>");
        }

        if ((buffer[0] == (byte) '\02') || (buffer[0] == (byte) '\01')) {
            String msg = new String(buffer, 1, i - 1);

            if (buffer[0] == (byte) '\02') {
                throw new IOException(msg);
            }

            throw new IOException("SCP returned an unexpected error: " + msg);
        }

        return new String(buffer, 0, i);
    }
}
