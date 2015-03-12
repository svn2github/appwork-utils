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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyConnectException;

/**
 * @author daniel
 *
 */
public class DirectSocketConnection extends SocketConnection {

    public DirectSocketConnection(HTTPProxy proxy) {
        super(proxy);
        if (proxy == null || !proxy.isLocal()) {
            throw new IllegalArgumentException("proxy must be of type none/direct");
        }
    }

    public DirectSocketConnection() {
        this(HTTPProxy.NONE);
    }

    @Override
    public void connect(SocketAddress endpoint, final int connectTimeout, final StringBuffer logger) throws IOException {
        try {
            try {
                if (connectTimeout == 0) {
                    /** no workaround for infinite connect timeouts **/
                    final Socket connectSocket = this.createConnectSocket(connectTimeout);
                    connectSocket.connect(endpoint, connectTimeout);
                } else {
                    /**
                     * workaround for too early connect timeouts
                     */
                    int connectTimeoutWorkaround = connectTimeout;
                    while (true) {
                        final long beforeConnect = System.currentTimeMillis();
                        try {
                            final Socket connectSocket = this.createConnectSocket(connectTimeout);
                            connectSocket.connect(endpoint, connectTimeout);
                            break;
                        } catch (final ConnectException cE) {
                            closeConnectSocket();
                            if (StringUtils.containsIgnoreCase(cE.getMessage(), "timed out")) {
                                int timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                if (timeout < 1000) {
                                    System.out.println("Too Fast ConnectTimeout(Normal): " + timeout + "->Wait " + (2000 - timeout));
                                    try {
                                        Thread.sleep(2000 - timeout);
                                    } catch (final InterruptedException ie) {
                                        throw cE;
                                    }
                                    timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                }
                                final int lastConnectTimeout = connectTimeoutWorkaround;
                                connectTimeoutWorkaround = Math.max(0, connectTimeoutWorkaround - timeout);
                                if (connectTimeoutWorkaround == 0 || Thread.currentThread().isInterrupted()) {
                                    throw cE;
                                }
                                System.out.println("Workaround for ConnectTimeout(Normal): " + lastConnectTimeout + ">" + timeout);
                            } else {
                                throw cE;
                            }
                        } catch (final SocketTimeoutException sTE) {
                            closeConnectSocket();
                            if (StringUtils.containsIgnoreCase(sTE.getMessage(), "timed out")) {
                                int timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                if (timeout < 1000) {
                                    System.out.println("Too Fast ConnectTimeout(Interrupted): " + timeout + "->Wait " + (2000 - timeout));
                                    try {
                                        Thread.sleep(2000 - timeout);
                                    } catch (final InterruptedException ie) {
                                        throw sTE;
                                    }
                                    timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                }
                                final int lastConnectTimeout = connectTimeoutWorkaround;
                                connectTimeoutWorkaround = Math.max(0, connectTimeoutWorkaround - timeout);
                                if (connectTimeoutWorkaround == 0 || Thread.currentThread().isInterrupted()) {
                                    throw sTE;
                                }
                                System.out.println("Workaround for ConnectTimeout(Interrupted): " + lastConnectTimeout + ">" + timeout);
                            } else {
                                throw sTE;
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                throw new ProxyConnectException(e, this.getProxy());
            }
            final Socket connectedSocket = this.connectProxySocket(this.getConnectSocket(), endpoint, logger);
            if (connectedSocket != null) {
                this.proxySocket = connectedSocket;
                return;
            }
            throw new ProxyConnectException(this.getProxy());
        } catch (final IOException e) {
            if (e instanceof ProxyConnectException) {
                throw e;
            }
            throw new ProxyConnectException(e, this.getProxy());
        } finally {
            if (this.proxySocket == null) {
                this.closeConnectSocket();
            }
        }
    }

    @Override
    protected Socket createConnectSocket(int connectTimeout) throws IOException {
        final Socket socket = super.createConnectSocket(connectTimeout);
        if (this.getProxy().isDirect()) {
            try {
                socket.bind(new InetSocketAddress(HTTPConnectionImpl.getDirectInetAddress(getProxy()), 0));
            } catch (IOException e) {
                socket.close();
                throw e;
            }
        }
        return socket;
    }

    @Override
    protected Socket connectProxySocket(Socket proxySocket, SocketAddress endpoint, StringBuffer logger) throws IOException {
        return proxySocket;
    }
}
