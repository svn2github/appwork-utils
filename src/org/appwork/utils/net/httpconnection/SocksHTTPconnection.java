/**
 * Copyright (c) 2009 - 2012 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpconnection
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.SSLSocket;

/**
 * @author daniel
 *
 */
public abstract class SocksHTTPconnection extends HTTPConnectionImpl {

    public static enum AUTH {
        PLAIN,
        NONE
    }

    public static enum DESTTYPE {
        IPV4,
        DOMAIN
    }

    protected Socket            sockssocket            = null;

    protected int               httpPort;
    protected String            httpHost;
    protected StringBuffer      proxyRequest           = null;
    protected InetSocketAddress proxyInetSocketAddress = null;

    public SocksHTTPconnection(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
    }

    @Override
    public void connect() throws IOException {
        /* establish to destination through socks */
        this.httpPort = this.httpURL.getPort();
        this.httpHost = this.httpURL.getHost();
        if (this.httpPort == -1) {
            this.httpPort = this.httpURL.getDefaultPort();
        }
        boolean sslSNIWorkAround = false;
        connect: while (true) {
            if (this.isConnectionSocketValid()) { return;/* oder fehler */
            }
            this.resetConnection();
            this.proxyRequest = new StringBuffer();
            try {
                long startTime = System.currentTimeMillis();
                this.sockssocket = this.establishConnection();
                if (this.httpURL.getProtocol().startsWith("https")) {
                    /* we need to lay ssl over normal socks5 connection */
                    try {
                        final SSLSocket sslSocket;
                        if (sslSNIWorkAround) {
                            /* wrong configured SNI at serverSide */
                            sslSocket = (SSLSocket) HTTPConnectionImpl.getSSLSocketFactory(this).createSocket(this.sockssocket, "", this.httpPort, true);
                        } else {
                            sslSocket = (SSLSocket) HTTPConnectionImpl.getSSLSocketFactory(this).createSocket(this.sockssocket, this.httpURL.getHost(), this.httpPort, true);
                        }
                        sslSocket.startHandshake();
                        this.verifySSLHostname(sslSocket);
                        this.connectionSocket = sslSocket;
                    } catch (final IOException e) {
                        this.connectExceptions.add(this.sockssocket + "|" + e.getMessage());
                        this.disconnect();
                        if (sslSNIWorkAround == false && e.getMessage().contains("unrecognized_name")) {
                            sslSNIWorkAround = true;
                            continue connect;
                        }
                        throw new ProxyConnectException(e, this.proxy);
                    }
                } else {
                    /* we can continue to use the socks connection */
                    this.connectionSocket = this.sockssocket;
                }
                this.setReadTimeout(this.readTimeout);
                this.httpResponseCode = -1;
                this.requestTime = System.currentTimeMillis() - startTime;
                this.httpPath = new org.appwork.utils.Regex(this.httpURL.toString(), "https?://.*?(/.+)").getMatch(0);
                if (this.httpPath == null) {
                    this.httpPath = "/";
                }
                /* now send Request */
                this.sendRequest();
                return;
            } catch (final javax.net.ssl.SSLException e) {
                this.connectExceptions.add(this.proxyInetSocketAddress + "|" + e.getMessage());
                this.disconnect();
                if (sslSNIWorkAround == false && e.getMessage().contains("unrecognized_name")) {
                    sslSNIWorkAround = true;
                    continue connect;
                }
                throw new ProxyConnectException(e, this.proxy);
            } catch (final IOException e) {
                this.disconnect();
                if (e instanceof HTTPProxyException) { throw e; }
                this.connectExceptions.add(this.proxyInetSocketAddress + "|" + e.getMessage());
                throw new ProxyConnectException(e, this.proxy);
            }
        }
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        try {
            this.readTimeout = Math.max(0, readTimeout);
            this.sockssocket.setSoTimeout(this.readTimeout);
            this.connectionSocket.setSoTimeout(this.readTimeout);
        } catch (final Throwable ignore) {
        }
    }

    @Override
    protected boolean isKeepAlivedEnabled() {
        return false;
    }

    @Override
    public void disconnect() {
        try {
            super.disconnect();
        } finally {
            try {
                if (this.sockssocket != null) {
                    this.sockssocket.close();
                }
            } catch (final Throwable e) {
                this.sockssocket = null;
            }
        }
    }

    abstract protected Socket establishConnection() throws IOException;

    @Override
    protected String getRequestInfo() {
        if (this.proxyRequest != null) {
            final StringBuilder sb = new StringBuilder();
            final String type = this.proxy.getType().name();
            sb.append("-->" + type + ":").append(this.proxy.getHost() + ":" + this.proxy.getPort()).append("\r\n");
            if (this.proxyInetSocketAddress != null && this.proxyInetSocketAddress.getAddress() != null) {
                sb.append("-->" + type + "IP:").append(this.proxyInetSocketAddress.getAddress().getHostAddress()).append("\r\n");
            }
            sb.append("----------------CONNECTRequest(" + type + ")----------\r\n");
            sb.append(this.proxyRequest.toString());
            sb.append("------------------------------------------------\r\n");
            sb.append(super.getRequestInfo());
            return sb.toString();
        }
        return super.getRequestInfo();
    }

}
