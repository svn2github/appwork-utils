/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.socketconnection
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.socketconnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyConnectException;
import org.appwork.utils.net.httpconnection.SocksHTTPconnection.DESTTYPE;

/**
 * @author daniel
 *
 */
public class Socks4SocketConnection extends SocketConnection {

    private final DESTTYPE destType;

    public DESTTYPE getDestType() {
        return this.destType;
    }

    public Socks4SocketConnection(HTTPProxy proxy, DESTTYPE destType) {
        super(proxy);
        if (proxy == null || !HTTPProxy.TYPE.SOCKS4.equals(proxy.getType())) { throw new IllegalArgumentException("proxy must be of type socks4"); }
        this.destType = destType;
    }

    @Override
    protected Socket connectProxySocket(final Socket proxySocket, final SocketAddress endpoint, final StringBuffer logger) throws IOException {
        try {
            Socks4SocketConnection.sayHello(proxySocket, logger);
        } catch (final IOException e) {
            throw new ProxyConnectException(e, this.getProxy());
        }
        try {
            return Socks4SocketConnection.establishConnection(proxySocket, this.getProxy().getUser(), endpoint, this.getDestType(), logger);
        } catch (final IOException e) {
            throw new ProxyConnectException(e, this.getProxy());
        }
    }

    public static Socket establishConnection(final Socket proxySocket, final String userID, final SocketAddress endpoint, DESTTYPE destType, final StringBuffer logger) throws IOException {
        final InetSocketAddress endPointAddress = (InetSocketAddress) endpoint;
        final OutputStream os = proxySocket.getOutputStream();
        os.write((byte) 1);
        /* send port */
        /* network byte order */
        final int port = endPointAddress.getPort();
        os.write(port >> 8 & 0xff);
        os.write(port & 0xff);
        /* send ipv4/domain */
        switch (destType) {
        case IPV4:
            final InetAddress address = endPointAddress.getAddress();
            if (address != null) {
                if (logger != null) {
                    logger.append("->SEND tcp connect request by ipv4:" + address.getHostAddress() + "\r\n");
                }
                os.write(address.getAddress());
                break;
            } else {
                if (logger != null) {
                    logger.append("->Cannot connect request by ipv4 (unresolved)\r\n");
                }
            }
        case DOMAIN:
            destType = DESTTYPE.DOMAIN;
            /* we use domain */
            os.write((byte) 0);
            os.write((byte) 0);
            os.write((byte) 0);
            os.write((byte) 100);
            if (logger != null) {
                logger.append("->SEND tcp connect request by domain:" + SocketConnection.getHostName(endPointAddress) + "\r\n");
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported destType");
        }
        /* send user ID string */
        if (userID != null && userID.length() > 0) {
            os.write(userID.getBytes("ISO-8859-1"));
        }
        /* NULL/end */
        os.write((byte) 0);
        if (DESTTYPE.DOMAIN.equals(destType)) {
            final byte[] domainBytes = SocketConnection.getHostName(endPointAddress).getBytes("ISO-8859-1");
            /* send domain as string,socks4a */
            os.write(domainBytes);
            /* NULL/end */
            os.write((byte) 0);
        }
        os.flush();
        /* read response, 8 bytes */
        final InputStream is = proxySocket.getInputStream();
        final byte[] resp = SocketConnection.ensureRead(is, 2, null);
        if (resp[0] != 0) { throw new IOException("Invalid response:" + resp[0]); }
        switch (resp[1]) {
        case 0x5a:
            break;
        case 0x5b:
            throw new SocketException("Socks4 request rejected or failed");
        case 0x5c:
            throw new SocketException("Socks4 request failed because client is not running identd (or not reachable from the server)");
        case 0x5d:
            throw new ConnectException("Socks4 request failed because client's identd could not confirm the user ID string in the request");
        default:
            throw new IOException("Socks4 could not establish connection, status=" + resp[1]);
        }
        /* port */
        final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
        /* ip4v response */
        final byte[] connectedIP = SocketConnection.ensureRead(is, 4, null);
        if (logger != null) {
            logger.append("<-BOUND IP:" + InetAddress.getByAddress(connectedIP) + ":" + ByteBuffer.wrap(connectedPort).getShort() + "\r\n");
        }
        return proxySocket;
    }

    public static void sayHello(final Socket proxySocket, final StringBuffer logger) throws IOException {
        final OutputStream os = proxySocket.getOutputStream();
        if (logger != null) {
            logger.append("->SOCKS4 Hello\r\n");
        }
        /* socks5 */
        os.write((byte) 4);
    }

}
