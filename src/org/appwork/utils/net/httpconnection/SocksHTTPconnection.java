/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

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

    protected SocketStreamInterface sockssocket            = null;

    protected int                   httpPort;
    protected StringBuffer          proxyRequest           = null;
    protected InetSocketAddress     proxyInetSocketAddress = null;

    public SocksHTTPconnection(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
    }

    @Override
    public void connect() throws IOException {
        /* establish to destination through socks */
        this.httpPort = this.httpURL.getPort();
        if (this.httpPort == -1) {
            this.httpPort = this.httpURL.getDefaultPort();
        }
        if (!isHostnameResolved()) {
            setHostname(resolveHostname(httpURL.getHost()));
        }
        boolean sslSNIWorkAround = false;
        connect: while (true) {
            if (this.isConnectionSocketValid()) {
                return;/* oder fehler */
            }
            this.resetConnection();
            this.proxyRequest = new StringBuffer();
            try {
                final long startTime = System.currentTimeMillis();
                this.sockssocket = this.createConnectionSocket(null);
                this.sockssocket = connect(sockssocket);
                if (this.httpURL.getProtocol().startsWith("https")) {
                    /* we need to lay ssl over normal socks5 connection */
                    try {
                        final SSLSocketStreamFactory factory = getSSLSocketStreamFactory();
                        if (sslSNIWorkAround) {
                            /* wrong configured SNI at serverSide */
                            this.connectionSocket = factory.create(sockssocket, "", httpPort, true, isSSLTrustALL());
                        } else {
                            this.connectionSocket = factory.create(sockssocket, getHostname(), httpPort, true, isSSLTrustALL());
                        }
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
                this.connectTime = System.currentTimeMillis() - startTime;
                /* now send Request */
                this.sendRequest();
                return;
            } catch (final javax.net.ssl.SSLException e) {
                try {
                    this.connectExceptions.add(this.proxyInetSocketAddress + "|" + e.getMessage());
                } finally {
                    this.disconnect();
                }
                if (sslSNIWorkAround == false && e.getMessage().contains("unrecognized_name")) {
                    sslSNIWorkAround = true;
                    continue connect;
                }
                throw new ProxyConnectException(e, this.proxy);
            } catch (final IOException e) {
                try {
                    this.connectExceptions.add(this.proxyInetSocketAddress + "|" + e.getMessage());
                } finally {
                    this.disconnect();
                }
                if (e instanceof HTTPProxyException) {
                    throw e;
                } else {
                    throw new ProxyConnectException(e, this.proxy);
                }
            }
        }
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        try {
            this.readTimeout = Math.max(0, readTimeout);
            final SocketStreamInterface sockssocket = this.sockssocket;
            if (sockssocket != null) {
                this.sockssocket.getSocket().setSoTimeout(this.readTimeout);
            }
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
            final SocketStreamInterface sockssocket = this.sockssocket;
            try {
                if (sockssocket != null) {
                    sockssocket.close();
                }
            } catch (final Throwable e) {
                this.sockssocket = null;
            }
        }
    }

    abstract protected SocketStreamInterface connect(SocketStreamInterface socketStream) throws IOException;

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
