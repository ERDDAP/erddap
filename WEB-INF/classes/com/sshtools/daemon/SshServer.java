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
package com.sshtools.daemon;

import com.sshtools.daemon.authentication.*;
import com.sshtools.daemon.configuration.*;
import com.sshtools.daemon.transport.*;

import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.connection.*;
import com.sshtools.j2ssh.net.*;
import com.sshtools.j2ssh.transport.*;
import com.sshtools.j2ssh.util.*;

import org.apache.commons.logging.*;

import java.io.*;

import java.net.*;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.18 $
 */
public abstract class SshServer {
    private static Log log = LogFactory.getLog(SshServer.class);
    private ConnectionListener listener = null;
    private ServerSocket server = null;
    private boolean shutdown = false;
    private ServerSocket commandServerSocket;

    /**  */
    protected List activeConnections = new Vector();
    Thread thread;

    /**
 * Creates a new SshServer object.
 *
 * @throws IOException
 * @throws SshException
 */
    public SshServer() throws IOException {
        String serverId = System.getProperty("sshtools.serverid");

        if (serverId != null) {
            TransportProtocolServer.SOFTWARE_VERSION_COMMENTS = serverId;
        }

        if (!ConfigurationLoader.isConfigurationAvailable(
                    ServerConfiguration.class)) {
            throw new SshException("Server configuration not available!");
        }

        if (!ConfigurationLoader.isConfigurationAvailable(
                    PlatformConfiguration.class)) {
            throw new SshException("Platform configuration not available");
        }

        if (((ServerConfiguration) ConfigurationLoader.getConfiguration(
                    ServerConfiguration.class)).getServerHostKeys().size() <= 0) {
            throw new SshException(
                "Server cannot start because there are no server host keys available");
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void startServer() throws IOException {
        log.info("Starting server");
        shutdown = false;
        startServerSocket();
        thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            startCommandSocket();
                        } catch (IOException ex) {
                            log.info("Failed to start command socket", ex);

                            try {
                                stopServer("The command socket failed to start");
                            } catch (IOException ex1) {
                            }
                        }
                    }
                });
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
 *
 *
 * @param command
 * @param client
 *
 * @throws IOException
 */
    protected void processCommand(int command, Socket client)
        throws IOException {
        if (command == 0x3a) {
            int len = client.getInputStream().read();
            byte[] msg = new byte[len];
            client.getInputStream().read(msg);
            stopServer(new String(msg));
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    protected void startCommandSocket() throws IOException {
        try {
            commandServerSocket = new ServerSocket(((ServerConfiguration) ConfigurationLoader.getConfiguration(
                        ServerConfiguration.class)).getCommandPort(), 50,
                    InetAddress.getLocalHost());

            Socket client;

            while ((client = commandServerSocket.accept()) != null) {
                log.info("Command request received");

                // Read and process the command
                processCommand(client.getInputStream().read(), client);
                client.close();

                if (shutdown) {
                    break;
                }
            }

            commandServerSocket.close();
        } catch (Exception e) {
            if (!shutdown) {
                log.fatal("The command socket failed", e);
            }
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    protected void startServerSocket() throws IOException {
        listener = new ConnectionListener(((ServerConfiguration) ConfigurationLoader.getConfiguration(
                    ServerConfiguration.class)).getListenAddress(),
                ((ServerConfiguration) ConfigurationLoader.getConfiguration(
                    ServerConfiguration.class)).getPort());
        listener.start();
    }

    /**
 *
 *
 * @param msg
 *
 * @throws IOException
 */
    public void stopServer(String msg) throws IOException {
        log.info("Shutting down server");
        shutdown = true;
        log.debug(msg);
        shutdown(msg);
        listener.stop();
        log.debug("Stopping command server");

        try {
            if (commandServerSocket != null) {
                commandServerSocket.close();
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
    }

    /**
 *
 *
 * @param msg
 */
    protected abstract void shutdown(String msg);

    /**
 *
 *
 * @param connection
 *
 * @throws IOException
 */
    protected abstract void configureServices(ConnectionProtocol connection)
        throws IOException;

    /**
 *
 *
 * @param socket
 *
 * @throws IOException
 */
    protected void refuseSession(Socket socket) throws IOException {
        TransportProtocolServer transport = new TransportProtocolServer(true);
        transport.startTransportProtocol(new ConnectedSocketTransportProvider(
                socket), new SshConnectionProperties());
    }

    /**
 *
 *
 * @param socket
 *
 * @return
 *
 * @throws IOException
 */
    protected TransportProtocolServer createSession(Socket socket)
        throws IOException {
        log.debug("Initializing connection");

        InetAddress address = socket.getInetAddress();

        /*( (InetSocketAddress) socket
     .getRemoteSocketAddress()).getAddress();*/
        log.debug("Remote Hostname: " + address.getHostName());
        log.debug("Remote IP: " + address.getHostAddress());

        TransportProtocolServer transport = new TransportProtocolServer();

        // Create the Authentication Protocol
        AuthenticationProtocolServer authentication = new AuthenticationProtocolServer();

        // Create the Connection Protocol
        ConnectionProtocol connection = new ConnectionProtocol();

        // Configure the connections services
        configureServices(connection);

        // Allow the Connection Protocol to be accepted by the Authentication Protocol
        authentication.acceptService(connection);

        // Allow the Authentication Protocol to be accepted by the Transport Protocol
        transport.acceptService(authentication);
        transport.startTransportProtocol(new ConnectedSocketTransportProvider(
                socket), new SshConnectionProperties());

        return transport;
    }

    class ConnectionListener implements Runnable {
        private Log log = LogFactory.getLog(ConnectionListener.class);
        private ServerSocket server;
        private String listenAddress;
        private Thread thread;
        private int maxConnections;
        private int port;
        private StartStopState state = new StartStopState(StartStopState.STOPPED);

        public ConnectionListener(String listenAddress, int port) {
            this.port = port;
            this.listenAddress = listenAddress;
        }

        public void run() {
            try {
                log.debug("Starting connection listener thread");
                state.setValue(StartStopState.STARTED);
                server = new ServerSocket(port);

                Socket socket;
                maxConnections = ((ServerConfiguration) ConfigurationLoader.getConfiguration(ServerConfiguration.class)).getMaxConnections();

                boolean refuse = false;
                TransportProtocolEventHandler eventHandler = new TransportProtocolEventAdapter() {
                        public void onDisconnect(TransportProtocol transport) {
                            // Remove from our active channels list only if
                            // were still connected (the thread cleans up
                            // when were exiting so this is to avoid any concurrent
                            // modification problems
                            if (state.getValue() != StartStopState.STOPPED) {
                                synchronized (activeConnections) {
                                    log.info(transport.getUnderlyingProviderDetail() +
                                        " connection closed");
                                    activeConnections.remove(transport);
                                }
                            }
                        }
                    };

                try {
                    while (((socket = server.accept()) != null) &&
                            (state.getValue() == StartStopState.STARTED)) {
                        log.debug("New connection requested");

                        if (maxConnections < activeConnections.size()) {
                            refuseSession(socket);
                        } else {
                            TransportProtocolServer transport = createSession(socket);
                            log.info("Monitoring active session from " +
                                socket.getInetAddress().getHostName());

                            synchronized (activeConnections) {
                                activeConnections.add(transport);
                            }

                            transport.addEventHandler(eventHandler);
                        }
                    }
                } catch (IOException ex) {
                    if (state.getValue() != StartStopState.STOPPED) {
                        log.info("The server was shutdown unexpectedly", ex);
                    }
                }

                state.setValue(StartStopState.STOPPED);

                // Closing all connections
                log.info("Disconnecting active sessions");

                for (Iterator it = activeConnections.iterator(); it.hasNext();) {
                    ((TransportProtocolServer) it.next()).disconnect(
                        "The server is shuting down");
                }

                listener = null;
                log.info("Exiting connection listener thread");
            } catch (IOException ex) {
                log.info("The server thread failed", ex);
            } finally {
                thread = null;
            }

            // brett
            //      System.exit(0);
        }

        public void start() {
            thread = new SshThread(this, "Connection listener", true);
            thread.start();
        }

        public void stop() {
            try {
                state.setValue(StartStopState.STOPPED);
                server.close();

                if (thread != null) {
                    thread.interrupt();
                }
            } catch (IOException ioe) {
                log.warn("The listening socket reported an error during shutdown",
                    ioe);
            }
        }
    }
}
