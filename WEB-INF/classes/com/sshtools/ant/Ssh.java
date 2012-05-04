/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter
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
package com.sshtools.ant;

import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.authentication.*;
import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.session.*;
import com.sshtools.j2ssh.transport.*;
import com.sshtools.j2ssh.transport.cipher.*;
import com.sshtools.j2ssh.transport.hmac.*;
import com.sshtools.j2ssh.transport.publickey.*;

import org.apache.tools.ant.*;

import java.io.*;

import java.util.*;


public class Ssh extends Task {
    protected String host;
    protected int port = 22;
    protected String username;
    protected String password;
    protected String keyfile;
    protected String passphrase;
    protected String cipher;
    protected String mac;
    protected String fingerprint;
    protected String logfile = null;
    protected boolean verifyhost = true;
    protected boolean always = false;
    protected SshClient ssh;
    protected Vector tasks = new Vector();
    protected String sshtoolsHome;
    protected String newline = "\n";

    public Ssh() {
        super();
    }

    protected void validate() throws BuildException {
        if (host == null) {
            throw new BuildException("You must provide a host to connect to!");
        }

        if (username == null) {
            throw new BuildException(
                "You must supply a username for authentication!");
        }

        if ((password == null) && (keyfile == null)) {
            throw new BuildException(
                "You must supply either a password or keyfile/passphrase to authenticate!");
        }

        if (verifyhost && (fingerprint == null)) {
            throw new BuildException(
                "Public key fingerprint required to verify the host");
        }
    }

    protected void connectAndAuthenticate() throws BuildException {
        if (sshtoolsHome != null) {
            System.setProperty("sshtools.home", sshtoolsHome);
        }

        log("Initializing J2SSH");

        try {
            ConfigurationLoader.initialize(false);
            log("Creating connection to " + host + ":" + String.valueOf(port));

            if (ssh == null) {
                ssh = new SshClient();

                SshConnectionProperties properties = new SshConnectionProperties();
                properties.setHost(host);
                properties.setPort(port);
                properties.setUsername(username);

                if (cipher != null) {
                    if (SshCipherFactory.getSupportedCiphers().contains(cipher)) {
                        properties.setPrefSCEncryption(cipher);
                        properties.setPrefCSEncryption(cipher);
                    } else {
                        this.log(cipher +
                            " is not a supported cipher, using default " +
                            SshCipherFactory.getDefaultCipher());
                    }
                }

                if (mac != null) {
                    if (SshHmacFactory.getSupportedMacs().contains(mac)) {
                        properties.setPrefCSMac(mac);
                        properties.setPrefSCMac(mac);
                    } else {
                        this.log(mac +
                            " is not a supported mac, using default " +
                            SshHmacFactory.getDefaultHmac());
                    }
                }

                log("Connecting....");
                ssh.connect(properties,
                    new AbstractKnownHostsKeyVerification(
                        new File(System.getProperty("user.home"),
                            ".ssh" + File.separator + "known_hosts").getAbsolutePath()) {
                        public void onUnknownHost(String hostname,
                            SshPublicKey key) throws InvalidHostFileException {
                            if (Ssh.this.verifyhost) {
                                if (key.getFingerprint().equalsIgnoreCase(Ssh.this.fingerprint)) {
                                    allowHost(hostname, key, always);
                                }
                            } else {
                                allowHost(hostname, key, always);
                            }
                        }

                        public void onHostKeyMismatch(String hostname,
                            SshPublicKey allowed, SshPublicKey supplied)
                            throws InvalidHostFileException {
                            if (Ssh.this.verifyhost) {
                                if (supplied.getFingerprint().equalsIgnoreCase(Ssh.this.fingerprint)) {
                                    allowHost(hostname, supplied, always);
                                }
                            } else {
                                allowHost(hostname, supplied, always);
                            }
                        }

                        public void onDeniedHost(String host) {
                            log("The server host key is denied!");
                        }
                    });

                int result;
                boolean authenticated = false;
                log("Authenticating " + username);

                if (keyfile != null) {
                    log("Performing public key authentication");

                    PublicKeyAuthenticationClient pk = new PublicKeyAuthenticationClient();

                    // Open up the private key file
                    SshPrivateKeyFile file = SshPrivateKeyFile.parse(new File(
                                keyfile));

                    // If the private key is passphrase protected then ask for the passphrase
                    if (file.isPassphraseProtected() && (passphrase == null)) {
                        throw new BuildException(
                            "Private key file is passphrase protected, please supply a valid passphrase!");
                    }

                    // Get the key
                    SshPrivateKey key = file.toPrivateKey(passphrase);
                    pk.setUsername(username);
                    pk.setKey(key);

                    // Try the authentication
                    result = ssh.authenticate(pk);

                    if (result == AuthenticationProtocolState.COMPLETE) {
                        authenticated = true;
                    } else if (result == AuthenticationProtocolState.PARTIAL) {
                        log(
                            "Public key authentication completed, attempting password authentication");
                    } else {
                        throw new BuildException(
                            "Public Key Authentication failed!");
                    }
                }

                if ((password != null) && (authenticated == false)) {
                    log("Performing password authentication");

                    PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
                    pwd.setUsername(username);
                    pwd.setPassword(password);
                    result = ssh.authenticate(pwd);

                    if (result == AuthenticationProtocolState.COMPLETE) {
                        log("Authentication complete");
                    } else if (result == AuthenticationProtocolState.PARTIAL) {
                        throw new BuildException(
                            "Password Authentication succeeded but further authentication required!");
                    } else {
                        throw new BuildException(
                            "Password Authentication failed!");
                    }
                }
            }
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }

    protected void disconnect() throws BuildException {
        try {
            log("Disconnecting from " + host);
            ssh.disconnect();
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }

    public void execute() throws org.apache.tools.ant.BuildException {
        validate();
        connectAndAuthenticate();
        executeSubTasks();
        disconnect();
    }

    protected void executeSubTasks() throws BuildException {
        Iterator it = tasks.iterator();
        SshSubTask task;

        while (it.hasNext()) {
            task = (SshSubTask) it.next();
            task.setParent(this);
            task.execute(ssh);
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setKeyfile(String keyfile) {
        this.keyfile = keyfile;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public void setVerifyhost(boolean verifyhost) {
        this.verifyhost = verifyhost;
    }

    public void setAlways(boolean always) {
        this.always = always;
    }

    public void setSshtoolshome(String sshtoolsHome) {
        this.sshtoolsHome = sshtoolsHome;
    }

    protected boolean hasMoreSftpTasks() {
        Iterator it = tasks.iterator();

        while (it.hasNext()) {
            if (it.next().getClass().equals(Sftp.class)) {
                return true;
            }
        }

        return false;
    }

    public SshSubTask createShell() {
        SshSubTask task = new Shell();
        tasks.addElement(task);

        return task;
    }

    public SshSubTask createExec() {
        SshSubTask task = new Exec();
        tasks.addElement(task);

        return task;
    }

    public SshSubTask createSftp() {
        SshSubTask task = new Sftp();
        tasks.addElement(task);

        return task;
    }

    public class Exec extends Shell {
        private String cmd;

        public void execute(SshClient ssh) throws BuildException {
            this.validate();

            try {
                log("Executing command " + cmd);

                // Create the session channel
                SessionChannelClient session = ssh.openSessionChannel();
                output = new SessionOutputReader(session);

                // Allocate a pseudo terminal is one has been requested
                allocatePseudoTerminal(session);

                // Execute the command
                if (session.executeCommand(cmd)) {
                    performTasks(session);
                } else {
                    throw new BuildException("The command failed to start");
                }
            } catch (IOException ex) {
            }
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }
    }

    public class Shell extends SshSubTask implements PseudoTerminal {
        private String term = null;
        private int cols = 80;
        private int rows = 34;
        private int width = 0;
        private int height = 0;
        private String terminalModes = "";
        private Vector commands = new Vector();
        private SessionChannelClient session;
        protected SessionOutputReader output;

        public void execute(SshClient ssh) throws BuildException {
            this.validate();

            try {
                // Create the session channel
                session = ssh.openSessionChannel();

                // Add an event listener so we can filter the output for our read commands
                // This is much easier than reading from an InputStream that could potentailly block
                output = new SessionOutputReader(session);

                // Allocate a pseudo terminal is one has been requested
                allocatePseudoTerminal(session);

                // Start the shell
                if (session.startShell()) {
                    performTasks(session);
                } else {
                    throw new BuildException("The session failed to start");
                }
            } catch (IOException ex) {
            }
        }

        protected void validate() throws BuildException {
            if (ssh == null) {
                throw new BuildException("Invalid SSH session");
            }

            if (!ssh.isConnected()) {
                throw new BuildException("The SSH session is not connected");
            }
        }

        protected void allocatePseudoTerminal(SessionChannelClient session)
            throws BuildException {
            try {
                if (term != null) {
                    if (!session.requestPseudoTerminal(this)) {
                        throw new BuildException(
                            "The server failed to allocate a pseudo terminal");
                    }
                }
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        }

        protected void performTasks(SessionChannelClient session)
            throws BuildException {
            if (commands.size() > 0) {
                Iterator it = commands.iterator();
                Object obj;

                while (it.hasNext()) {
                    obj = it.next();

                    if (obj instanceof Write) {
                        ((Write) obj).execute();
                    } else if (obj instanceof Read) {
                        ((Read) obj).execute();
                    } else {
                        throw new BuildException("Unexpected shell operation " +
                            obj.toString());
                    }
                }
            } else {
                try {
                    output.echoLineByLineToClose(new SessionOutputEcho() {
                            public void echo(String echo) {
                                log(echo);
                            }
                        });
                } catch (InterruptedException ex) {
                    throw new BuildException(ex);
                }
            }
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public void setCols(int cols) {
            this.cols = cols;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        /**
         * PseduoTermainal interface
         */
        public String getTerm() {
            return term;
        }

        public int getColumns() {
            return cols;
        }

        public int getRows() {
            return rows;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getEncodedTerminalModes() {
            return terminalModes;
        }

        /**
         * Reading/Writing to the session/command
         */
        public Write createWrite() {
            Write write = new Write();
            commands.add(write);

            return write;
        }

        public Read createRead() {
            Read read = new Read();
            commands.add(read);

            return read;
        }

        public class Read {
            protected String taskString = "";
            private int timeout = 0;
            private boolean echo = true;

            public void execute() throws BuildException {
                try {
                    output.markCurrentPosition();

                    if (output.waitForString(taskString, timeout,
                                new SessionOutputEcho() {
                                public void echo(String msg) {
                                    if (echo) {
                                    log(msg);
                                }
                            }
                        })
                    ) {
                    } else {
                        throw new BuildException("Timeout waiting for string " +
                            taskString);
                    }
                } catch (InterruptedException ex) {
                    throw new BuildException(ex);
                }
            }

            /**
             *  the message as nested text
             */
            public void addText(String s) {
                setString(Ssh.this.getProject().replaceProperties(s));
            }

            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }

            public void setEcho(boolean echo) {
                this.echo = echo;
            }

            /**
             * the message as an attribute
             */
            public void setString(String s) {
                taskString += s;
            }
        }

        public class Write {
            protected boolean echo = true;
            protected String taskString = "";
            protected boolean newline = true;

            public void execute() throws BuildException {
                try {
                    if (echo) {
                        log(taskString);
                    }

                    session.getOutputStream().write(taskString.getBytes());

                    if (newline) {
                        session.getOutputStream().write(Ssh.this.newline.getBytes());
                    }
                } catch (IOException ex) {
                    throw new BuildException(ex);
                }
            }

            /**
             *  the message as nested text
             */
            public void addText(String s) {
                setString(Ssh.this.getProject().replaceProperties(s));
            }

            /**
             * the message as an attribute
             */
            public void setString(String s) {
                taskString += s;
            }

            public void setEcho(boolean echo) {
                this.echo = echo;
            }

            public void setNewline(boolean newline) {
                this.newline = newline;
            }
        }
    }
}
