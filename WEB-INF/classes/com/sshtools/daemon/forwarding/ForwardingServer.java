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
package com.sshtools.daemon.forwarding;

import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelFactory;
import com.sshtools.j2ssh.connection.ConnectionProtocol;
import com.sshtools.j2ssh.connection.GlobalRequestHandler;
import com.sshtools.j2ssh.connection.GlobalRequestResponse;
import com.sshtools.j2ssh.connection.InvalidChannelException;
import com.sshtools.j2ssh.forwarding.ForwardingClient;
import com.sshtools.j2ssh.forwarding.ForwardingConfiguration;
import com.sshtools.j2ssh.forwarding.ForwardingConfigurationException;
import com.sshtools.j2ssh.forwarding.ForwardingListener;
import com.sshtools.j2ssh.forwarding.ForwardingSocketChannel;
import com.sshtools.j2ssh.io.ByteArrayReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

import java.net.Socket;
import java.net.SocketPermission;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class ForwardingServer implements ChannelFactory, GlobalRequestHandler {
    private static Log log = LogFactory.getLog(ForwardingServer.class);
    private ConnectionProtocol connection;
    private List channelTypes = new Vector();
    private List localForwardings = new Vector();
    private List remoteForwardings = new Vector();

    /**
 * Creates a new ForwardingServer object.
 *
 * @param connection
 *
 * @throws IOException
 */
    public ForwardingServer(ConnectionProtocol connection)
        throws IOException {
        this.connection = connection;
        channelTypes.add(ForwardingSocketChannel.LOCAL_FORWARDING_CHANNEL);
        connection.addChannelFactory(ForwardingSocketChannel.LOCAL_FORWARDING_CHANNEL,
            this);
        connection.allowGlobalRequest(ForwardingClient.REMOTE_FORWARD_REQUEST,
            this);
        connection.allowGlobalRequest(ForwardingClient.REMOTE_FORWARD_CANCEL_REQUEST,
            this);
    }

    /* public List getChannelType() {
 return channelTypes;
  }*/
    public Channel createChannel(String channelType, byte[] requestData)
        throws InvalidChannelException {
        if (!channelType.equals(
                    ForwardingSocketChannel.LOCAL_FORWARDING_CHANNEL)) {
            throw new InvalidChannelException(
                "The client can only request the " +
                "opening of a local forwarding channel");
        }

        try {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String hostToConnect = bar.readString();
            int portToConnect = (int) bar.readInt();
            String originatingHost = bar.readString();
            int originatingPort = (int) bar.readInt();

            // Get a configuration item for the forwarding
            ForwardingConfiguration config = getLocalForwardingByAddress(originatingHost,
                    originatingPort);
            Socket socket = new Socket(hostToConnect, portToConnect);

            // Create the channel adding it to the active channels
            ForwardingSocketChannel channel = config.createForwardingSocketChannel(channelType,
                    hostToConnect, portToConnect, originatingHost,
                    originatingPort);
            channel.bindSocket(socket);

            return channel;
        } catch (ForwardingConfigurationException fce) {
            throw new InvalidChannelException(
                "No valid forwarding configuration was available for the request");
        } catch (IOException ioe) {
            throw new InvalidChannelException("The channel request data is " +
                "invalid/or corrupt for channel type " + channelType);
        }
    }

    /**
 *
 *
 * @param requestName
 * @param requestData
 *
 * @return
 */
    public GlobalRequestResponse processGlobalRequest(String requestName,
        byte[] requestData) {
        GlobalRequestResponse response = new GlobalRequestResponse(false);
        String addressToBind = null;
        int portToBind = -1;
        log.debug("Processing " + requestName + " global request");

        try {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            addressToBind = bar.readString();
            portToBind = (int) bar.readInt();

            if (requestName.equals(ForwardingClient.REMOTE_FORWARD_REQUEST)) {
                addRemoteForwardingConfiguration(addressToBind, portToBind);
                response = new GlobalRequestResponse(true);
            }

            if (requestName.equals(
                        ForwardingClient.REMOTE_FORWARD_CANCEL_REQUEST)) {
                removeRemoteForwarding(addressToBind, portToBind);
                response = new GlobalRequestResponse(true);
            }
        } catch (IOException ioe) {
            log.warn("The client failed to request " + requestName + " for " +
                addressToBind + ":" + String.valueOf(portToBind), ioe);
        }

        return response;
    }

    /**
 *
 *
 * @param orginatingAddress
 * @param originatingPort
 *
 * @return
 *
 * @throws ForwardingConfigurationException
 */
    protected ForwardingConfiguration getLocalForwardingByAddress(
        String orginatingAddress, int originatingPort)
        throws ForwardingConfigurationException {
        try {
            Iterator it = localForwardings.iterator();
            ForwardingConfiguration config;

            while (it.hasNext()) {
                config = (ForwardingConfiguration) it.next();

                if (config.getAddressToBind().equals(orginatingAddress) &&
                        (config.getPortToBind() == originatingPort)) {
                    return config;
                }
            }

            // No configuration is available so create one
            config = new ForwardingConfiguration(orginatingAddress,
                    originatingPort);

            // We must start this configuration in order to use it
            config.start();
            localForwardings.add(config);

            return config;
        } catch (IOException ex) {
            throw new ForwardingConfigurationException(ex.getMessage());
        }
    }

    /**
 *
 *
 * @param addressToBind
 * @param portToBind
 *
 * @return
 *
 * @throws ForwardingConfigurationException
 */
    protected ForwardingConfiguration getRemoteForwardingByAddress(
        String addressToBind, int portToBind)
        throws ForwardingConfigurationException {
        Iterator it = remoteForwardings.iterator();
        ForwardingConfiguration config;

        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();

            if (config.getAddressToBind().equals(addressToBind) &&
                    (config.getPortToBind() == portToBind)) {
                return config;
            }
        }

        throw new ForwardingConfigurationException(
            "The remote forwarding does not exist!");
    }

    /**
 *
 *
 * @param addressToBind
 * @param portToBind
 *
 * @throws ForwardingConfigurationException
 */
    protected void addRemoteForwardingConfiguration(String addressToBind,
        int portToBind) throws ForwardingConfigurationException {
        // Is the server already listening
        Iterator it = remoteForwardings.iterator();
        ForwardingConfiguration config;

        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();

            if (config.getAddressToBind().equals(addressToBind) &&
                    (config.getPortToBind() == portToBind)) {
                throw new ForwardingConfigurationException(
                    "The address and port are already in use!");
            }
        }

        config = new ForwardingConfiguration(addressToBind, portToBind);

        // Check the security mananger
        SecurityManager manager = System.getSecurityManager();

        if (manager != null) {
            try {
                manager.checkPermission(new SocketPermission(addressToBind +
                        ":" + String.valueOf(portToBind), "accept,listen"));
            } catch (SecurityException e) {
                throw new ForwardingConfigurationException(
                    "The security manager has denied listen permision on " +
                    addressToBind + ":" + String.valueOf(portToBind));
            }
        }

        try {
            ForwardingListener listener = new ServerForwardingListener(connection,
                    addressToBind, portToBind);
            remoteForwardings.add(listener);
            listener.start();
        } catch (IOException ex) {
            throw new ForwardingConfigurationException(ex.getMessage());
        }
    }

    /**
 *
 *
 * @param addressToBind
 * @param portToBind
 *
 * @throws ForwardingConfigurationException
 */
    protected void removeRemoteForwarding(String addressToBind, int portToBind)
        throws ForwardingConfigurationException {
        ForwardingConfiguration config = getRemoteForwardingByAddress(addressToBind,
                portToBind);

        // Stop the forwarding
        config.stop();

        // Remove from the remote forwardings list
        remoteForwardings.remove(config);
    }

    class ServerForwardingListener extends ForwardingListener {
        public ServerForwardingListener(ConnectionProtocol connection,
            String addressToBind, int portToBind) {
            super(connection, addressToBind, portToBind);
        }

        public ForwardingSocketChannel createChannel(String hostToConnect,
            int portToConnect, Socket socket)
            throws ForwardingConfigurationException {
            try {
                ForwardingSocketChannel channel = createForwardingSocketChannel(ForwardingSocketChannel.REMOTE_FORWARDING_CHANNEL,
                        hostToConnect, portToConnect,
                        
                    /*( (InetSocketAddress) socket.getRemoteSocketAddress()).getAddress()
          .getHostAddress(),
          ( (InetSocketAddress) socket.getRemoteSocketAddress())
          .getPort()*/
                    socket.getInetAddress().getHostAddress(), socket.getPort());
                channel.bindSocket(socket);

                return channel;
            } catch (IOException ex) {
                throw new ForwardingConfigurationException(ex.getMessage());
            }
        }
    }
}
