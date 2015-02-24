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

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.net.httpconnection.ProxyConnectException;
import org.appwork.utils.net.httpconnection.SocksHTTPconnection.AUTH;
import org.appwork.utils.net.httpconnection.SocksHTTPconnection.DESTTYPE;

/**
 * @author daniel
 *
 */
public class Socks5SocketConnection extends SocketConnection {

    private final DESTTYPE destType;

    public DESTTYPE getDestType() {
        return this.destType;
    }

    public Socks5SocketConnection(HTTPProxy proxy, DESTTYPE destType) {
        super(proxy);
        if (proxy == null || !HTTPProxy.TYPE.SOCKS5.equals(proxy.getType())) {
            throw new IllegalArgumentException("proxy must be of type socks5");
        }
        this.destType = destType;
    }

    @Override
    protected Socket connectProxySocket(final Socket proxySocket, final SocketAddress endpoint, final StringBuffer logger) throws IOException {
        final AUTH authOffer;
        final String userName = this.getProxy().getUser();
        final String passWord = this.getProxy().getPass();
        if (!StringUtils.isEmpty(userName) || !StringUtils.isEmpty(passWord)) {
            authOffer = AUTH.PLAIN;
        } else {
            authOffer = AUTH.NONE;
        }
        final AUTH authRequest;
        try {
            authRequest = Socks5SocketConnection.sayHello(proxySocket, authOffer, logger);
        } catch (final IOException e) {
            throw new ProxyConnectException(e, this.getProxy());
        }
        switch (authRequest) {
        case PLAIN:
            switch (authOffer) {
            case NONE:
                throw new ProxyAuthException(this.getProxy());
            case PLAIN:
                try {
                    Socks5SocketConnection.authPlain(proxySocket, userName, passWord, logger);
                } catch (final IOException e) {
                    throw new ProxyAuthException(e, this.getProxy());
                }
            }
            break;
        default:
            break;
        }
        try {
            return Socks5SocketConnection.establishConnection(proxySocket, endpoint, this.getDestType(), logger);
        } catch (final IOException e) {
            throw new ProxyConnectException(e, this.getProxy());
        }
    }

    public static Socket establishConnection(final Socket proxySocket, final SocketAddress endpoint, DESTTYPE destType, final StringBuffer logger) throws IOException {
        final InetSocketAddress endPointAddress = (InetSocketAddress) endpoint;
        final OutputStream os = proxySocket.getOutputStream();
        /* socks5 */
        os.write((byte) 5);
        /* tcp/ip connection */
        os.write((byte) 1);
        /* reserved */
        os.write((byte) 0);
        /* send ipv4/domain */
        switch (destType) {
        case IPV4:
            final InetAddress address = endPointAddress.getAddress();
            if (address != null) {
                /* we use ipv4 */
                os.write((byte) 1);
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
            /* we use domain */
            os.write((byte) 3);
            final String domainString = SocketConnection.getHostName(endPointAddress);
            if (logger != null) {
                logger.append("->SEND tcp connect request by domain:" + domainString + "\r\n");
            }
            final byte[] domainBytes = domainString.getBytes("ISO-8859-1");
            os.write((byte) domainBytes.length);
            os.write(domainBytes);
            break;
        default:
            throw new IllegalArgumentException("Unsupported destType");
        }
        /* send port */
        /* network byte order */
        final int port = endPointAddress.getPort();
        os.write(port >> 8 & 0xff);
        os.write(port & 0xff);
        os.flush();
        /* read response, 4 bytes and then read rest of response */
        final InputStream is = proxySocket.getInputStream();
        final byte[] resp = SocketConnection.ensureRead(is, 4, null);
        if (resp[0] != 5) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        switch (resp[1]) {
        case 0:
            break;
        case 3:
            throw new SocketException("Network is unreachable");
        case 4:
            throw new SocketException("Host is unreachable");
        case 5:
            throw new ConnectException("Connection refused");
        case 1:
            throw new IOException("Socks5 general server failure");
        case 2:
            throw new IOException("Socks5 connection not allowed by ruleset");
        case 6:
        case 7:
        case 8:
            throw new IOException("Socks5 could not establish connection, status=" + resp[1]);
        }
        if (resp[3] == 1) {
            /* ip4v response */
            final byte[] connectedIP = SocketConnection.ensureRead(is, 4, null);
            /* port */
            final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
            if (logger != null) {
                logger.append("<-BOUND IP:" + InetAddress.getByAddress(connectedIP) + ":" + ByteBuffer.wrap(connectedPort).getShort() + "\r\n");
            }
        } else if (resp[3] == 3) {
            /* domain name response */
            final byte[] length = SocketConnection.ensureRead(is, 1, null);
            final byte[] connectedDomain = SocketConnection.ensureRead(is, length[0], null);
            /* port */
            final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
            if (logger != null) {
                logger.append("<-BOUND Domain:" + new String(connectedDomain) + ":" + ByteBuffer.wrap(connectedPort).getShort() + "\r\n");
            }
        } else {
            throw new IOException("Socks5 unsupported address Type " + resp[3]);
        }

        return proxySocket;
    }

    public static void authPlain(final Socket proxySocket, String userName, String passWord, final StringBuffer logger) throws IOException {
        final String user = userName == null ? "" : userName;
        final String pass = passWord == null ? "" : passWord;
        if (logger != null) {
            logger.append("->AUTH user:pass\r\n");
        }
        final byte[] userNameBytes = user.getBytes("ISO-8859-1");
        final byte[] passWordBytes = pass.getBytes("ISO-8859-1");
        final OutputStream os = proxySocket.getOutputStream();
        /* must be 1 */
        os.write((byte) 1);
        /* send username */
        os.write((byte) userNameBytes.length);
        if (userNameBytes.length > 0) {
            os.write(userNameBytes);
        }
        /* send password */
        os.write((byte) passWordBytes.length);
        if (passWordBytes.length > 0) {
            os.write(passWordBytes);
        }
        /* read response, 2 bytes */
        final InputStream is = proxySocket.getInputStream();
        final byte[] resp = SocketConnection.ensureRead(is, 2, null);
        if (resp[0] != 1) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        if (resp[1] != 0) {
            if (logger != null) {
                logger.append("<-AUTH Invalid!\r\n");
            }
            throw new IOException("Socks5 auth invalid");
        } else {
            if (logger != null) {
                logger.append("<-AUTH Valid!\r\n");
            }
        }
    }

    private final static boolean SENDONLYSINGLEAUTHMETHOD = true;

    public static AUTH sayHello(final Socket proxySocket, AUTH auth, final StringBuffer logger) throws IOException {
        final OutputStream os = proxySocket.getOutputStream();
        if (logger != null) {
            logger.append("->SOCKS5 Hello\r\n");
        }
        /* socks5 */
        os.write((byte) 5);
        /* only none ans password/username auth method */
        final boolean plainAuthPossible = AUTH.PLAIN.equals(auth);
        if (plainAuthPossible) {
            if (SENDONLYSINGLEAUTHMETHOD) {
                os.write((byte) 1);
                if (logger != null) {
                    logger.append("->SOCKS5 Offer Plain Authentication\r\n");
                }
                /* username/password */
                os.write((byte) 2);
            } else {
                os.write((byte) 2);
                if (logger != null) {
                    logger.append("->SOCKS5 Offer None&Plain Authentication\r\n");
                }
                /* none */
                os.write((byte) 0);
                /* username/password */
                os.write((byte) 2);
            }
        } else {
            os.write((byte) 1);
            if (logger != null) {
                logger.append("->SOCKS5 Offer None Authentication\r\n");
            }
            /* none */
            os.write((byte) 0);
        }
        os.flush();
        /* read response, 2 bytes */
        final InputStream is = proxySocket.getInputStream();
        final byte[] resp = SocketConnection.ensureRead(is, 2, null);
        if (resp[0] != 5) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        if (resp[1] == 255) {
            if (logger != null) {
                logger.append("<-SOCKS5 Authentication Denied\r\n");
            }
            throw new IOException("Socks5HTTPConnection: no acceptable authentication method found");
        }
        if (resp[1] == 2) {
            if (!plainAuthPossible && logger != null) {
                logger.append("->SOCKS5 Plain auth required but not offered!\r\n");
            }
            return AUTH.PLAIN;
        }
        if (resp[1] == 0) {
            return AUTH.NONE;
        }
        throw new IOException("Unsupported auth:" + resp[1]);
    }

}
