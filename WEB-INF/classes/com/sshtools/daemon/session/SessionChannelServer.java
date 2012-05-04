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
package com.sshtools.daemon.session;

import com.sshtools.daemon.configuration.*;
import com.sshtools.daemon.platform.*;
import com.sshtools.daemon.scp.*;
import com.sshtools.daemon.subsystem.*;

import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.agent.*;
import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.connection.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.util.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.16 $
 */
public class SessionChannelServer extends IOChannel {
    private static Log log = LogFactory.getLog(SessionChannelServer.class);

    /**  */
    public final static String SESSION_CHANNEL_TYPE = "session";
    private static Map allowedSubsystems = new HashMap();
    private Map environment = new HashMap();
    private NativeProcessProvider processInstance;
    private SubsystemServer subsystemInstance;
    private Thread thread;
    private IOStreamConnector ios;
    private ChannelOutputStream stderrOut;
    private InputStream stderrIn;
    private ProcessMonitorThread processMonitor;
    private PseudoTerminalWrapper pty;
    private SshAgentForwardingListener agent;
    private ServerConfiguration config;

    /**
 * Creates a new SessionChannelServer object.
 *
 * @throws ConfigurationException
 */
    public SessionChannelServer() throws ConfigurationException {
        super();

        // Load the allowed subsystems from the server configuration
        config = (ServerConfiguration) ConfigurationLoader.getConfiguration(ServerConfiguration.class);
        allowedSubsystems.putAll(config.getSubsystems());
    }

    private void bindStderrInputStream(InputStream stderrIn) {
        this.stderrIn = stderrIn;
        ios = new IOStreamConnector(stderrIn, stderrOut);
    }

    /**
 *
 *
 * @param cols
 * @param rows
 * @param width
 * @param height
 */
    protected void onChangeTerminalDimensions(int cols, int rows, int width,
        int height) {
    }

    /**
 *
 *
 * @throws IOException
 */
    protected void onChannelClose() throws IOException {
        // Remove our reference to the agent
        if (agent != null) {
            agent.removeReference(this);
        }

        if (processInstance != null) {
            if (processInstance.stillActive()) {
                processInstance.kill();
            }
        }

        if (subsystemInstance != null) {
            subsystemInstance.stop();
        }

        // If we have a process monitor then get the exit code
        // and send before we close the channel
        if (processMonitor != null) {
            StartStopState state = processMonitor.getState();

            try {
                state.waitForState(StartStopState.STOPPED);
            } catch (InterruptedException ex) {
                throw new IOException("The process monitor was interrupted");
            }
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    protected void onChannelEOF() throws IOException {
    }

    /**
 *
 *
 * @param data
 *
 * @throws IOException
 */
    protected void onChannelExtData(byte[] data) throws IOException {
        // Do something with the data
    }

    /**
 *
 *
 * @throws InvalidChannelException
 */
    protected void onChannelOpen() throws InvalidChannelException {
        stderrOut = new ChannelOutputStream(this,
                new Integer(SshMsgChannelExtendedData.SSH_EXTENDED_DATA_STDERR));
    }

    /**
 *
 *
 * @param command
 *
 * @return
 *
 * @throws IOException
 */
    protected boolean onExecuteCommand(String command)
        throws IOException {
        log.debug("Executing command " + command);

        // Hack for now
        if (command.startsWith("scp ")) {
            if (processInstance == null) {
                processInstance = new ScpServer();
            }
        }

        // Create an instance of the native process provider if we n
        if (processInstance == null) {
            processInstance = NativeProcessProvider.newInstance();
        }

        if (processInstance == null) {
            log.debug("Failed to create process");

            return false;
        }

        boolean result = processInstance.createProcess(command, environment);

        if (result) {
            if (pty != null) {
                // Bind the streams to the pseudo terminal wrapper
                pty.bindMasterOutputStream(getOutputStream());
                pty.bindMasterInputStream(getInputStream());
                pty.bindSlaveInputStream(processInstance.getInputStream());
                pty.bindSlaveOutputStream(processInstance.getOutputStream());

                // Initialize the terminal
                pty.initialize();

                // Bind the master output stream of the pty to the session
                bindInputStream(pty.getMasterInputStream());

                // Bind the processes stderr
                bindStderrInputStream(processInstance.getStderrInputStream());
            } else {
                // Just bind the process streams to the session
                bindInputStream(processInstance.getInputStream());
                bindOutputStream(processInstance.getOutputStream());
                bindStderrInputStream(processInstance.getStderrInputStream());
            }
        }

        return result;
    }

    /**
 *
 *
 * @param term
 * @param cols
 * @param rows
 * @param width
 * @param height
 * @param modes
 *
 * @return
 */
    protected boolean onRequestPseudoTerminal(String term, int cols, int rows,
        int width, int height, String modes) {
        try {
            // Create an instance of the native process provider
            processInstance = NativeProcessProvider.newInstance();

            if (processInstance.supportsPseudoTerminal(term)) {
                return processInstance.allocatePseudoTerminal(term, cols, rows,
                    width, height, modes);
            } else {
                pty = new PseudoTerminalWrapper(term, cols, rows, width,
                        height, modes);

                return true;
            }
        } catch (IOException ioe) {
            log.warn("Failed to allocate pseudo terminal " + term, ioe);

            return false;
        }
    }

    /**
 *
 *
 * @param name
 * @param value
 */
    protected void onSetEnvironmentVariable(String name, String value) {
        environment.put(name, value);
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    protected boolean onStartShell() throws IOException {
        String shell = config.getTerminalProvider();

        if (processInstance == null) {
            processInstance = NativeProcessProvider.newInstance();
        }

        if ((shell != null) && !shell.trim().equals("")) {
            int idx = shell.indexOf("%DEFAULT_TERMINAL%");

            if (idx > -1) {
                shell = ((idx > 0) ? shell.substring(0, idx) : "") +
                    processInstance.getDefaultTerminalProvider() +
                    (((idx + 18) < shell.length()) ? shell.substring(idx + 18)
                                                   : "");
            }
        } else {
            shell = processInstance.getDefaultTerminalProvider();
        }

        return onExecuteCommand(shell);
    }

    /**
 *
 *
 * @param subsystem
 *
 * @return
 */
    protected boolean onStartSubsystem(String subsystem) {
        boolean result = false;

        try {
            if (!allowedSubsystems.containsKey(subsystem)) {
                log.error(subsystem + " Subsystem is not available");

                return false;
            }

            AllowedSubsystem obj = (AllowedSubsystem) allowedSubsystems.get(subsystem);

            if (obj.getType().equals("class")) {
                // Create the class implementation and start the subsystem
                Class cls = Class.forName(obj.getProvider());
                subsystemInstance = (SubsystemServer) cls.newInstance();
                subsystemInstance.setSession(this);
                bindInputStream(subsystemInstance.getInputStream());
                bindOutputStream(subsystemInstance.getOutputStream());

                return true;
            } else {
                // Determine the subsystem provider
                String provider = obj.getProvider();
                File f = new File(provider);

                if (!f.exists()) {
                    provider = ConfigurationLoader.getHomeDirectory() + "bin" +
                        File.separator + provider;
                    f = new File(provider);

                    if (!f.exists()) {
                        log.error("Failed to locate subsystem provider " +
                            obj.getProvider());

                        return false;
                    }
                }

                return onExecuteCommand(provider);
            }
        } catch (Exception e) {
            log.error("Failed to start subsystem " + subsystem, e);
        }

        return false;
    }

    /**
 *
 *
 * @return
 */
    public byte[] getChannelOpenData() {
        return null;
    }

    /**
 *
 *
 * @return
 */
    public byte[] getChannelConfirmationData() {
        return null;
    }

    /**
 *
 *
 * @return
 */
    protected int getMinimumWindowSpace() {
        return 1024;
    }

    /**
 *
 *
 * @return
 */
    protected int getMaximumWindowSpace() {
        return 32648;
    }

    /**
 *
 *
 * @return
 */
    protected int getMaximumPacketSize() {
        return 32648;
    }

    /**
 *
 *
 * @return
 */
    public String getChannelType() {
        return SESSION_CHANNEL_TYPE;
    }

    /**
 *
 *
 * @param requestType
 * @param wantReply
 * @param requestData
 *
 * @throws IOException
 */
    protected void onChannelRequest(String requestType, boolean wantReply,
        byte[] requestData) throws IOException {
        log.debug("Channel Request received: " + requestType);

        boolean success = false;

        if (requestType.equals("shell")) {
            success = onStartShell();

            if (success) {
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }

                processInstance.start();
                processMonitor = new ProcessMonitorThread(processInstance);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }

        if (requestType.equals("env")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String name = bar.readString();
            String value = bar.readString();
            onSetEnvironmentVariable(name, value);

            if (wantReply) {
                connection.sendChannelRequestSuccess(this);
            }
        }

        if (requestType.equals("exec")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String command = bar.readString();
            success = onExecuteCommand(command);

            if (success) {
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }

                processInstance.start();
                processMonitor = new ProcessMonitorThread(processInstance);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }

        if (requestType.equals("subsystem")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String subsystem = bar.readString();
            success = onStartSubsystem(subsystem);

            if (success) {
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }

                if (processInstance != null) {
                    processInstance.start();
                    processMonitor = new ProcessMonitorThread(processInstance);
                } else if (subsystemInstance != null) {
                    subsystemInstance.start();
                    processMonitor = new ProcessMonitorThread(subsystemInstance);
                }
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }

        if (requestType.equals("pty-req")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String term = bar.readString();
            int cols = (int) bar.readInt();
            int rows = (int) bar.readInt();
            int width = (int) bar.readInt();
            int height = (int) bar.readInt();
            String modes = bar.readString();
            success = onRequestPseudoTerminal(term, cols, rows, width, height,
                    modes);

            if (wantReply && success) {
                connection.sendChannelRequestSuccess(this);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }

        if (requestType.equals("window-change")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            int cols = (int) bar.readInt();
            int rows = (int) bar.readInt();
            int width = (int) bar.readInt();
            int height = (int) bar.readInt();
            onChangeTerminalDimensions(cols, rows, width, height);

            if (wantReply && success) {
                connection.sendChannelRequestSuccess(this);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }

        if (requestType.equals("auth-agent-req")) {
            try {
                SshThread thread = SshThread.getCurrentThread();

                // Get an agent instance
                agent = SshAgentForwardingListener.getInstance(thread.getSessionIdString(),
                        connection);

                // Inform the agent we want to track this reference
                agent.addReference(this);

                // Set the environment so processes can find the agent
                environment.put("SSH_AGENT_AUTH", agent.getConfiguration());

                // Set a thread property so other services within this server can find it
                thread.setProperty("sshtools.agent", agent.getConfiguration());

                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }
            } catch (Exception ex) {
                if (wantReply) {
                    connection.sendChannelRequestFailure(this);
                }
            }
        }
    }

    class ProcessMonitorThread extends Thread {
        private NativeProcessProvider process;
        private SubsystemServer subsystem;
        private StartStopState state;

        public ProcessMonitorThread(NativeProcessProvider process) {
            this.process = process;
            state = new StartStopState(StartStopState.STARTED);
            start();
        }

        public ProcessMonitorThread(SubsystemServer subsystem) {
            state = subsystem.getState();
        }

        public StartStopState getState() {
            return state;
        }

        public void run() {
            try {
                log.info("Monitor waiting for process exit code");

                int exitcode = process.waitForExitCode();

                if (exitcode == 9999999) {
                    log.error("Process monitor failed to retrieve exit code");
                } else {
                    log.debug("Process exit code is " +
                        String.valueOf(exitcode));
                    process.getInputStream().close();
                    process.getOutputStream().close();
                    process.getStderrInputStream().close();

                    ByteArrayWriter baw = new ByteArrayWriter();
                    baw.writeInt(exitcode);

                    // Send the exit request
                    if (connection.isConnected() &&
                            SessionChannelServer.this.isOpen()) {
                        connection.sendChannelRequest(SessionChannelServer.this,
                            "exit-status", false, baw.toByteArray());
                    }

                    // Stop the monitor
                    state.setValue(StartStopState.STOPPED);

                    // Close the session
                    SessionChannelServer.this.close();
                }
            } catch (IOException ioe) {
                log.error("Failed to kill process", ioe);
            }
        }
    }
}
