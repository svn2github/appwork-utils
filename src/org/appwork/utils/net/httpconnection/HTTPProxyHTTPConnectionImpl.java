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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;

public class HTTPProxyHTTPConnectionImpl extends HTTPConnectionImpl {
    private int                 httpPort;
    private StringBuilder       proxyRequest;
    private final boolean       preferConnectMethod;
    protected InetSocketAddress proxyInetSocketAddress = null;

    public HTTPProxyHTTPConnectionImpl(final URL url, final HTTPProxy p) {
        super(url, p);
        this.preferConnectMethod = p.isConnectMethodPrefered();
        this.setRequestProperty("Proxy-Connection", "close");
        if (!url.getProtocol().startsWith("https") && !preferConnectMethod) {
            this.httpPath = getRequestPath(url, true);
        }
    }

    private boolean isProxySupported(final HTTPProxy p) {
        return p != null && (HTTPProxy.TYPE.HTTP.equals(p.getType()) || HTTPProxy.TYPE.HTTPS.equals(p.getType()));
    }

    /*
     * SSL over HTTP Proxy, see http://muffin.doit.org/docs/rfc/tunneling_ssl.html
     */
    @Override
    public void connect() throws IOException {
        boolean sslSNIWorkAround = false;
        InetAddress hosts[] = null;
        connect: while (true) {
            if (this.isConnectionSocketValid()) {
                return;/* oder fehler */
            }
            this.resetConnection();
            if (!isHostnameResolved()) {
                setHostname(resolveHostname(httpURL.getHost()));
            }
            try {
                if (!isProxySupported(proxy)) {
                    throw new IOException("HTTPProxyHTTPConnection: proxy unsupported");
                }
                if (this.proxy.getPass() != null && this.proxy.getPass().length() > 0 || this.proxy.getUser() != null && this.proxy.getUser().length() > 0) {
                    /* add proxy auth in case username/pw are set */
                    final String user = this.proxy.getUser() == null ? "" : this.proxy.getUser();
                    final String pass = this.proxy.getPass() == null ? "" : this.proxy.getPass();
                    this.requestProperties.put("Proxy-Authorization", "Basic " + new String(Base64.encodeToByte((user + ":" + pass).getBytes(), false)));
                }
                if (hosts == null) {
                    if (StringUtils.isEmpty(proxy.getHost())) {
                        throw new ProxyConnectException(new UnknownHostException("Could not resolve: -empty host-"), this.proxy);
                    }
                    hosts = this.resolvHostIP(this.proxy.getHost());
                }
                IOException ee = null;
                long startTime = System.currentTimeMillis();
                for (final InetAddress host : hosts) {
                    this.resetConnection();
                    startTime = System.currentTimeMillis();
                    this.connectionSocket = createConnectionSocket(null);
                    try {
                        /* create and connect to socks5 proxy */
                        this.connectionSocket.getSocket().connect(this.proxyInetSocketAddress = new InetSocketAddress(host, this.proxy.getPort()), this.connectTimeout);
                        /* connection is okay */
                        ee = null;
                        break;
                    } catch (final IOException e) {
                        this.disconnect();
                        this.connectExceptions.add(this.proxyInetSocketAddress + "|" + e.getMessage());
                        /* connection failed, try next available ip */
                        ee = e;
                    }
                }
                if (ee != null) {
                    throw new ProxyConnectException(ee, this.proxy);
                }
                if (HTTPProxy.TYPE.HTTPS.equals(proxy.getType())) {
                    final SSLSocketStreamFactory factory = getSSLSocketStreamFactory();
                    try {
                        this.connectionSocket = factory.create(connectionSocket, "", proxy.getPort(), true, isSSLTrustALL());
                    } catch (final IOException e) {
                        this.connectExceptions.add(this.connectionSocket + "|" + e.getMessage());
                        this.disconnect();
                        throw new ProxyConnectException(e, this.proxy);
                    }
                }
                this.connectTime = System.currentTimeMillis() - startTime;
                if (this.httpURL.getProtocol().startsWith("https") || this.isConnectMethodPrefered()) {
                    /* ssl via CONNECT method or because we prefer CONNECT */
                    /* build CONNECT request */
                    this.proxyRequest = new StringBuilder();
                    this.proxyRequest.append("CONNECT ");
                    this.proxyRequest.append(getHostname() + ":" + (this.httpURL.getPort() != -1 ? this.httpURL.getPort() : this.httpURL.getDefaultPort()));
                    this.proxyRequest.append(" HTTP/1.1\r\n");
                    if (this.requestProperties.get("User-Agent") != null) {
                        this.proxyRequest.append("User-Agent: " + this.requestProperties.get("User-Agent") + "\r\n");
                    }
                    if (this.requestProperties.get("Host") != null) {
                        /* use existing host header */
                        this.proxyRequest.append("Host: " + this.requestProperties.get("Host") + "\r\n");
                    } else {
                        /* add host from url as fallback */
                        this.proxyRequest.append("Host: " + this.httpURL.getHost() + "\r\n");
                    }
                    if (this.requestProperties.get("Proxy-Authorization") != null) {
                        this.proxyRequest.append("Proxy-Authorization: " + this.requestProperties.get("Proxy-Authorization") + "\r\n");
                    }
                    this.proxyRequest.append("\r\n");
                    /* send CONNECT to proxy */
                    this.connectionSocket.getOutputStream().write(this.proxyRequest.toString().getBytes("UTF-8"));
                    this.connectionSocket.getOutputStream().flush();
                    /* parse CONNECT response */
                    ByteBuffer header = HTTPConnectionUtils.readheader(this.connectionSocket.getInputStream(), true);
                    byte[] bytes = new byte[header.limit()];
                    header.get(bytes);
                    final String proxyResponseStatus = new String(bytes, "ISO-8859-1").trim();
                    this.proxyRequest.append(proxyResponseStatus + "\r\n");
                    String proxyCode = null;
                    if (proxyResponseStatus.startsWith("HTTP")) {
                        /* parse response code */
                        proxyCode = new Regex(proxyResponseStatus, "HTTP.*? (\\d+)").getMatch(0);
                    }
                    if (!"200".equals(proxyCode)) {
                        /* something went wrong */
                        try {
                            this.connectionSocket.close();
                        } catch (final Throwable nothing) {
                        }
                        if ("407".equals(proxyCode)) {
                            /* auth invalid/missing */
                            throw new ProxyAuthException(this.proxy);
                        }
                        throw new ProxyConnectException(this.proxy);
                    }
                    /* read rest of CONNECT headers */
                    /*
                     * Again, the response follows the HTTP/1.0 protocol, so the response line starts with the protocol version specifier,
                     * and the response line is followed by zero or more response headers, followed by an empty line. The line separator is
                     * CR LF pair, or a single LF.
                     */
                    while (true) {
                        /*
                         * read line by line until we reach the single empty line as separator
                         */
                        header = HTTPConnectionUtils.readheader(this.connectionSocket.getInputStream(), true);
                        if (header.limit() <= 2) {
                            /* empty line, <=2, as it may contains \r and/or \n */
                            break;
                        }
                        bytes = new byte[header.limit()];
                        header.get(bytes);
                        final String temp = fromBytes(bytes, -1, -1);
                        this.proxyRequest.append(temp + "\r\n");
                    }
                    this.httpPort = this.httpURL.getPort();
                    if (this.httpPort == -1) {
                        this.httpPort = this.httpURL.getDefaultPort();
                    }
                    if (this.httpURL.getProtocol().startsWith("https")) {
                        try {
                            final SSLSocketStreamFactory factory = getSSLSocketStreamFactory();
                            if (sslSNIWorkAround) {
                                /* wrong configured SNI at serverSide */
                                this.connectionSocket = factory.create(connectionSocket, "", httpPort, true, isSSLTrustALL());
                            } else {
                                this.connectionSocket = factory.create(connectionSocket, getHostname(), httpPort, true, isSSLTrustALL());
                            }
                        } catch (final IOException e) {
                            this.connectExceptions.add(this.connectionSocket + "|" + e.getMessage());
                            this.disconnect();
                            if (sslSNIWorkAround == false && e.getMessage().contains("unrecognized_name")) {
                                sslSNIWorkAround = true;
                                continue connect;
                            }
                            throw new ProxyConnectException(e, this.proxy);
                        }
                    }
                    /*
                     * httpPath needs to be like normal http request, eg /index.html
                     */
                } else {
                    /* direct connect via proxy */
                    /*
                     * httpPath needs to include complete path here, eg http://google.de/
                     */
                    this.proxyRequest = new StringBuilder("DIRECT\r\n");
                }
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
    protected boolean isKeepAlivedEnabled() {
        return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        this.connect();
        this.connectInputStream();
        if (this.getResponseCode() == 405) {
            // 405 - Method not allowed
            throw new ProxyConnectException(this.getResponseCode() + " " + this.getResponseMessage(), getProxy());
        }
        if (this.getResponseCode() == 407) {
            /* auth invalid/missing */
            throw new ProxyAuthException(this.proxy);
        }
        if (this.getResponseCode() == 502 && StringUtils.containsIgnoreCase(getResponseMessage(), "ISA Server denied the specified")) {
            throw new ProxyConnectException(this.getResponseCode() + " " + this.getResponseMessage(), getProxy());
        }
        if (this.getResponseCode() == 504) {
            throw new ProxyConnectException(this.getResponseCode() + " " + this.getResponseMessage(), getProxy());
        }
        return super.getInputStream();
    }

    @Override
    protected String getRequestInfo() {
        if (this.proxyRequest != null) {
            final StringBuilder sb = new StringBuilder();
            if (HTTPProxy.TYPE.HTTPS.equals(proxy.getType())) {
                sb.append("-->HTTPSProxy:").append(this.proxy.getHost()).append(":").append(this.proxy.getPort()).append("\r\n");
            } else {
                sb.append("-->HTTPProxy:").append(this.proxy.getHost()).append(":").append(this.proxy.getPort()).append("\r\n");
            }
            if (this.proxyInetSocketAddress != null && this.proxyInetSocketAddress.getAddress() != null) {
                if (HTTPProxy.TYPE.HTTPS.equals(proxy.getType())) {
                    sb.append("-->HTTPSProxyIP:").append(this.proxyInetSocketAddress.getAddress().getHostAddress()).append("\r\n");
                } else {
                    sb.append("-->HTTPProxyIP:").append(this.proxyInetSocketAddress.getAddress().getHostAddress()).append("\r\n");
                }
            }
            if (HTTPProxy.TYPE.HTTPS.equals(proxy.getType())) {
                sb.append("----------------CONNECTRequest(HTTPS)------------\r\n");
            } else {
                sb.append("----------------CONNECTRequest(HTTP)------------\r\n");
            }
            sb.append(this.proxyRequest.toString());
            sb.append("------------------------------------------------\r\n");
            sb.append(super.getRequestInfo());
            return sb.toString();
        }
        return super.getRequestInfo();
    }

    public boolean isConnectMethodPrefered() {
        return this.preferConnectMethod;
    }
}
