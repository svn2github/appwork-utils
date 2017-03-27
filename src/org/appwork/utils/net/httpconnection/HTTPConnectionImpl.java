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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.ChunkedInputStream;
import org.appwork.utils.net.CountingOutputStream;
import org.appwork.utils.net.LimitedInputStream;
import org.appwork.utils.net.PublicSuffixList;
import org.appwork.utils.net.SocketFactory;
import org.appwork.utils.net.StreamValidEOF;
import org.appwork.utils.os.CrossSystem;

public class HTTPConnectionImpl implements HTTPConnection {
    public static enum KEEPALIVE {
        /**
         * KEEP-ALIVE is disabled
         */
        DISABLED,
        /**
         * KEEP-ALIVE is enabled for GET/HEAD/OPTIONS/DELETE
         */
        /* ENABLED_INTERNAL, */
        /**
         * KEEP-ALIVE is enabled, caller must handle HTTPKeepAliveSocketException
         */
        EXTERNAL_EXCEPTION
    }

    /**
     *
     */
    public static final String            UNKNOWN_HTTP_RESPONSE         = "unknown HTTP response";
    private static SSLSocketStreamFactory defaultSSLSocketStreamFactory = null;

    public static void setDefaultSSLSocketStreamFactory(SSLSocketStreamFactory defaultSSLSocketStreamFactory) {
        HTTPConnectionImpl.defaultSSLSocketStreamFactory = defaultSSLSocketStreamFactory;
    }

    public static SSLSocketStreamFactory getDefaultSSLSocketStreamFactory() {
        final SSLSocketStreamFactory ret = defaultSSLSocketStreamFactory;
        if (ret != null) {
            return ret;
        } else {
            return JavaSSLSocketStreamFactory.getInstance();
        }
    }

    protected HTTPHeaderMap<String>          requestProperties = null;
    protected volatile long[]                ranges;
    protected String                         customcharset     = null;
    protected volatile SocketStreamInterface connectionSocket  = null;

    protected SocketStreamInterface getConnectionSocket() {
        return this.connectionSocket;
    }

    protected final URL       httpURL;
    protected final HTTPProxy proxy;

    public HTTPProxy getProxy() {
        return this.proxy;
    }

    protected String                      httpPath;
    protected RequestMethod               httpMethod          = RequestMethod.GET;
    protected HTTPHeaderMap<List<String>> headers             = null;
    protected int                         httpResponseCode    = -1;
    protected String                      httpResponseMessage = "";
    protected volatile int                readTimeout         = 30000;
    protected volatile int                connectTimeout      = 30000;

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    protected volatile long                      connectTime          = -1;
    protected volatile long                      requestTime          = -1;
    protected OutputStream                       outputStream         = null;
    protected InputStream                        inputStream          = null;
    protected InputStream                        convertedInputStream = null;
    protected volatile boolean                   inputStreamConnected = false;
    protected String                             httpHeader           = null;
    protected String                             invalidHttpHeader    = null;
    private boolean                              contentDecoded       = true;
    protected long                               postTodoLength       = -1;
    private int[]                                allowedResponseCodes = new int[0];
    protected final CopyOnWriteArrayList<String> connectExceptions    = new CopyOnWriteArrayList<String>();
    protected volatile KEEPALIVE                 keepAlive            = KEEPALIVE.DISABLED;
    protected volatile InetAddress               remoteIPs[]          = null;
    protected boolean                            sslTrustALL          = true;
    protected InetAddress                        lastConnection       = null;
    protected int                                lastConnectionPort   = -1;
    protected String                             hostName;
    private final static PublicSuffixList        PSL                  = PublicSuffixList.getInstance();

    public KEEPALIVE getKeepAlive() {
        return this.keepAlive;
    }

    public void setKeepAlive(KEEPALIVE keepAlive) {
        if (keepAlive == null) {
            keepAlive = KEEPALIVE.DISABLED;
        }
        this.keepAlive = keepAlive;
    }

    /**
     * Keep-Alive stuff
     */
    protected static final HashMap<String, LinkedList<HTTPKeepAliveSocket>>        KEEPALIVEPOOL         = new HashMap<String, LinkedList<HTTPKeepAliveSocket>>();
    protected static final WeakHashMap<SocketStreamInterface, HTTPKeepAliveSocket> KEEPALIVESOCKETS      = new WeakHashMap<SocketStreamInterface, HTTPKeepAliveSocket>();
    protected static final Object                                                  LOCK                  = new Object();
    protected static final DelayedRunnable                                         KEEPALIVECLEANUPTIMER = new DelayedRunnable(10000, 30000) {
        @Override
        public void delayedrun() {
            cleanupKeepAlivePools();
        }
    };

    private static final void cleanupKeepAlivePools() {
        synchronized (HTTPConnectionImpl.LOCK) {
            try {
                HTTPConnectionImpl.KEEPALIVESOCKETS.isEmpty();
                final Iterator<Entry<String, LinkedList<HTTPKeepAliveSocket>>> hostIterator = HTTPConnectionImpl.KEEPALIVEPOOL.entrySet().iterator();
                while (hostIterator.hasNext()) {
                    final Entry<String, LinkedList<HTTPKeepAliveSocket>> next = hostIterator.next();
                    final LinkedList<HTTPKeepAliveSocket> keepAliveSockets = next.getValue();
                    if (keepAliveSockets != null) {
                        final Iterator<HTTPKeepAliveSocket> keepAliveIterator = keepAliveSockets.iterator();
                        while (keepAliveIterator.hasNext()) {
                            final HTTPKeepAliveSocket keepAliveSocket = keepAliveIterator.next();
                            final SocketStreamInterface socketStream = keepAliveSocket.getSocketStream();
                            final Socket socket = socketStream.getSocket();
                            if (socket.isClosed() || keepAliveSocket.getKeepAliveTimestamp() <= System.currentTimeMillis()) {
                                try {
                                    socket.close();
                                } catch (final Throwable ignore) {
                                }
                                keepAliveIterator.remove();
                            }
                        }
                    }
                    if (keepAliveSockets == null || keepAliveSockets.size() == 0) {
                        hostIterator.remove();
                    }
                }
            } finally {
                if (HTTPConnectionImpl.KEEPALIVEPOOL.size() > 0) {
                    HTTPConnectionImpl.KEEPALIVECLEANUPTIMER.resetAndStart();
                }
            }
        }
    }

    public HTTPConnectionImpl(final URL url) {
        this(url, null);
    }

    public HTTPConnectionImpl(final URL url, final HTTPProxy p) {
        this.httpURL = url;
        this.proxy = p;
        this.requestProperties = new HTTPHeaderMap<String>();
        this.headers = new HTTPHeaderMap<List<String>>();
        this.httpPath = getRequestPath(url, false);
    }

    protected String getRequestPath(final URL url, final boolean includeAll) {
        final String httpPath = new org.appwork.utils.Regex(url.toString(), "https?://.*?(/.+)").getMatch(0);
        final String ret;
        if (httpPath == null) {
            ret = "/";
        } else {
            ret = httpPath;
        }
        if (includeAll) {
            final StringBuilder sb = new StringBuilder();
            sb.append(url.getProtocol());
            sb.append("://");
            sb.append(resolveHostname(url.getHost()));
            if (url.getPort() != -1) {
                sb.append(":").append(url.getPort());
            }
            sb.append(ret);
            return sb.toString();
        } else {
            return ret;
        }
    }

    protected long getDefaultKeepAliveMaxRequests() {
        return 128;
    }

    protected long getMaxKeepAliveSockets() {
        if (CrossSystem.getOS() == CrossSystem.OperatingSystem.WINDOWS_XP) {
            return 1;
        } else {
            return 5;
        }
    }

    protected long getDefaultKeepAliveTimeout() {
        return 60 * 1000l;
    }

    protected boolean isKeepAliveOK() {
        final int code = this.getResponseCode();
        return this.isOK() || code == 404 || code == 403 || code == 416;
    }

    protected boolean putKeepAliveSocket(final SocketStreamInterface socketStream) throws IOException {
        /**
         * only keep-Alive sockets if
         *
         * 1.) keepAliveEnabled, HTTP Request/Response signals Keep-Alive and keep-Alive feature is enabled
         *
         * 2.) responseCode is ok
         *
         * 3.) socket is open/not closed/input and output open
         *
         * 4.) used inputstream has reached valid EOF
         *
         * 5.) available outputstream has written all data
         *
         *
         */
        if (socketStream != null) {
            final Socket socket = socketStream.getSocket();
            if (socket != null && this.isKeepAlivedEnabled() && this.isKeepAliveOK() && socket.isConnected() && !socket.isClosed() && socket.isInputShutdown() == false && socket.isOutputShutdown() == false) {
                if (this.inputStream != null && this.inputStream instanceof StreamValidEOF && ((StreamValidEOF) this.inputStream).isValidEOF()) {
                    if (!this.isRequiresOutputStream() || ((CountingOutputStream) this.outputStream).transferedBytes() == this.postTodoLength) {
                        socket.setKeepAlive(true);
                        synchronized (HTTPConnectionImpl.LOCK) {
                            HTTPKeepAliveSocket keepAliveSocket = HTTPConnectionImpl.KEEPALIVESOCKETS.remove(socket);
                            if (keepAliveSocket == null) {
                                final String connectionResponse = this.getHeaderField("Keep-Alive");
                                final String maxKeepAliveTimeoutString = new Regex(connectionResponse, "timeout\\s*?=\\s*?(\\d+)").getMatch(0);
                                final String maxKeepAliveRequestsString = new Regex(connectionResponse, "max\\s*?=\\s*?(\\d+)").getMatch(0);
                                final long maxKeepAliveTimeout;
                                if (maxKeepAliveTimeoutString != null) {
                                    maxKeepAliveTimeout = Long.parseLong(maxKeepAliveTimeoutString) * 1000l;
                                } else {
                                    maxKeepAliveTimeout = this.getDefaultKeepAliveTimeout();
                                }
                                final long maxKeepAliveRequests;
                                if (maxKeepAliveRequestsString != null) {
                                    maxKeepAliveRequests = Long.parseLong(maxKeepAliveRequestsString);
                                } else {
                                    maxKeepAliveRequests = this.getDefaultKeepAliveMaxRequests();
                                }
                                final InetAddress localIP;
                                if (this.proxy != null && this.proxy.isDirect()) {
                                    localIP = socket.getLocalAddress();
                                } else {
                                    localIP = null;
                                }
                                final boolean ssl = StringUtils.equalsIgnoreCase("https", this.httpURL.getProtocol());
                                keepAliveSocket = new HTTPKeepAliveSocket(getHostname(), ssl, socketStream, maxKeepAliveTimeout, maxKeepAliveRequests, localIP, this.remoteIPs);
                            }
                            keepAliveSocket.increaseRequests();
                            if (keepAliveSocket.getRequestsLeft() > 0) {
                                String domain = null;
                                if (HTTPConnectionImpl.PSL != null) {
                                    domain = HTTPConnectionImpl.PSL.getDomain(keepAliveSocket.getHost());
                                }
                                if (StringUtils.isEmpty(domain)) {
                                    domain = "FALLBACK";
                                }
                                LinkedList<HTTPKeepAliveSocket> keepAlivePool = HTTPConnectionImpl.KEEPALIVEPOOL.get(domain);
                                if (keepAlivePool == null) {
                                    keepAlivePool = new LinkedList<HTTPKeepAliveSocket>();
                                    HTTPConnectionImpl.KEEPALIVEPOOL.put(domain, keepAlivePool);
                                }
                                keepAlivePool.add(keepAliveSocket);
                                keepAliveSocket.keepAlive();
                                final long maxKeepAlive = this.getMaxKeepAliveSockets();
                                if (keepAlivePool.size() > maxKeepAlive) {
                                    final Iterator<HTTPKeepAliveSocket> it = keepAlivePool.iterator();
                                    while (it.hasNext() && keepAlivePool.size() > maxKeepAlive) {
                                        final HTTPKeepAliveSocket next = it.next();
                                        try {
                                            next.getSocketStream().close();
                                        } catch (final Throwable ignore) {
                                        }
                                        it.remove();
                                    }
                                }
                                HTTPConnectionImpl.KEEPALIVECLEANUPTIMER.resetAndStart();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected SocketStreamInterface getKeepAliveSocket() throws IOException {
        final InetAddress localIP = getDirectInetAddress(getProxy());
        final int port;
        if (this.httpURL.getPort() == -1) {
            port = this.httpURL.getDefaultPort();
        } else {
            port = this.httpURL.getPort();
        }
        final String host = getHostname();
        final boolean ssl = StringUtils.equalsIgnoreCase("https", this.httpURL.getProtocol());
        String domain = null;
        if (HTTPConnectionImpl.PSL != null) {
            domain = HTTPConnectionImpl.PSL.getDomain(host);
        }
        if (StringUtils.isEmpty(domain)) {
            domain = "FALLBACK";
        }
        synchronized (HTTPConnectionImpl.LOCK) {
            final LinkedList<HTTPKeepAliveSocket> socketPool = HTTPConnectionImpl.KEEPALIVEPOOL.get(domain);
            if (socketPool != null) {
                final Iterator<HTTPKeepAliveSocket> socketPoolIterator = socketPool.descendingIterator();
                while (socketPoolIterator.hasNext()) {
                    final HTTPKeepAliveSocket next = socketPoolIterator.next();
                    final SocketStreamInterface socketStream = next.getSocketStream();
                    final Socket socket = socketStream.getSocket();
                    if (socket.isClosed() || next.getKeepAliveTimestamp() <= System.currentTimeMillis()) {
                        try {
                            socket.close();
                        } catch (final Throwable ignore) {
                        }
                        socketPoolIterator.remove();
                    } else if (socket.getPort() != port || !next.sameLocalIP(localIP)) {
                        continue;
                    } else if (next.isSsl() && ssl) {
                        /**
                         * ssl needs to have same hostname to avoid (SNI)
                         *
                         * <p>
                         * Your browser sent a request that this server could not understand.<br />
                         * Host name provided via SNI and via HTTP are different
                         * </p>
                         */
                        if (next.sameHost(host)) {
                            socketPoolIterator.remove();
                            HTTPConnectionImpl.KEEPALIVESOCKETS.put(socketStream, next);
                            return socketStream;
                        }
                    } else if (next.isSsl() == false && ssl == false && next.sameRemoteIPs(this.remoteIPs)) {
                        socketPoolIterator.remove();
                        HTTPConnectionImpl.KEEPALIVESOCKETS.put(socketStream, next);
                        return socketStream;
                    }
                }
                if (socketPool.isEmpty()) {
                    HTTPConnectionImpl.KEEPALIVEPOOL.remove(domain);
                }
            }
        }
        return null;
    }

    /* this will add Host header at the beginning */
    protected void addHostHeader() {
        final int defaultPort = this.httpURL.getDefaultPort();
        final int usedPort = this.httpURL.getPort();
        String port = "";
        if (usedPort != -1 && defaultPort != -1 && usedPort != defaultPort) {
            port = ":" + usedPort;
        }
        this.requestProperties.put("Host", getHostname() + port);
    }

    protected void resetConnection() {
        this.inputStreamConnected = false;
        this.httpResponseCode = -1;
        this.httpResponseMessage = "";
        this.postTodoLength = -1;
        this.outputStream = null;
        this.inputStream = null;
        this.convertedInputStream = null;
        this.connectTime = -1;
        this.requestTime = -1;
        this.headers.clear();
        this.ranges = null;
        this.lastConnection = null;
        this.lastConnectionPort = -1;
    }

    public static InetAddress getDirectInetAddress(HTTPProxy proxy) throws IOException {
        if (proxy != null && proxy.isDirect()) {
            final String local = proxy.getLocal();
            if (local != null && local.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                return InetAddress.getByName(local);
            }
            final String interfaceName = local;
            if (interfaceName != null) {
                final String ifName;
                final boolean subInterface;
                final int index = interfaceName.indexOf(":");
                if (index != -1) {
                    ifName = interfaceName.substring(0, index);
                    subInterface = true;
                } else {
                    ifName = interfaceName;
                    subInterface = false;
                }
                final NetworkInterface netif = NetworkInterface.getByName(ifName);
                if (netif == null) {
                    throw new ProxyConnectException("No such networkinterface: " + interfaceName, proxy);
                }
                if (!netif.isUp()) {
                    throw new ProxyConnectException("Unconnected networkinterface: " + interfaceName, proxy);
                }
                if (subInterface) {
                    final Enumeration<NetworkInterface> subif = netif.getSubInterfaces();
                    while (subif.hasMoreElements()) {
                        final NetworkInterface next = subif.nextElement();
                        if (interfaceName.equals(next.getName())) {
                            if (!next.isUp()) {
                                throw new ProxyConnectException("Unconnected networkinterface: " + interfaceName, proxy);
                            }
                            final Enumeration<InetAddress> iaSub = next.getInetAddresses();
                            while (iaSub.hasMoreElements()) {
                                final InetAddress ia = iaSub.nextElement();
                                if (ia instanceof Inet4Address) {
                                    return ia;
                                }
                            }
                            throw new ProxyConnectException("Unsupported networkinterface: " + interfaceName, proxy);
                        }
                    }
                    throw new ProxyConnectException("No such networkinterface: " + interfaceName, proxy);
                } else {
                    /**
                     * root.getInetAddresses contains all InetAddress (rootInterface+subInterfaces), so we have to filter out subInterfaces
                     * first
                     */
                    final HashSet<InetAddress> inetAddresses = new HashSet<InetAddress>();
                    final Enumeration<InetAddress> iaRoot = netif.getInetAddresses();
                    while (iaRoot.hasMoreElements()) {
                        final InetAddress ia = iaRoot.nextElement();
                        if (ia instanceof Inet4Address) {
                            inetAddresses.add(ia);
                        }
                    }
                    final Enumeration<NetworkInterface> subif = netif.getSubInterfaces();
                    while (subif.hasMoreElements()) {
                        final NetworkInterface next = subif.nextElement();
                        final Enumeration<InetAddress> iaSub = next.getInetAddresses();
                        while (iaSub.hasMoreElements()) {
                            final InetAddress ia = iaSub.nextElement();
                            if (ia instanceof Inet4Address) {
                                inetAddresses.remove(ia);
                            }
                        }
                    }
                    final Iterator<InetAddress> it = inetAddresses.iterator();
                    if (it.hasNext()) {
                        return it.next();
                    }
                }
                throw new ProxyConnectException("Unsupported networkinterface: " + interfaceName, proxy);
            }
            throw new ProxyConnectException("Invalid Direct Proxy", proxy);
        }
        return null;
    }

    protected Socket createRawConnectionSocket(final InetAddress bindInetAddress) throws IOException {
        Socket socket = SocketFactory.get().create(this, bindInetAddress);
        if (bindInetAddress != null) {
            try {
                socket.bind(new InetSocketAddress(bindInetAddress, 0));
            } catch (final IOException e) {
                try {
                    socket.close();
                } catch (final Throwable ignore) {
                }
                connectExceptions.add("Bind: " + bindInetAddress + "|" + e.getMessage());
                throw new ProxyConnectException(e, getProxy());
            }
        }
        // socket.setSoTimeout(readTimeout);
        return socket;
    }

    protected SocketStreamInterface createConnectionSocket(final InetAddress bindInetAddress) throws IOException {
        final SocketStreamInterface connectionSocket = getConnectionSocket();
        closeConnectionSocket(connectionSocket);
        final Socket socket = createRawConnectionSocket(bindInetAddress);
        return new SocketStreamInterface() {
            @Override
            public Socket getSocket() {
                return socket;
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return socket.getOutputStream();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return socket.getInputStream();
            }

            @Override
            public void close() throws IOException {
                socket.close();
            }
        };
    }

    private void closeConnectionSocket(final SocketStreamInterface connectionSocket) throws IOException {
        if (connectionSocket != null && this.connectionSocket == connectionSocket) {
            this.connectionSocket = null;
            connectionSocket.close();
        }
    }

    protected String resolveHostname(final String hostName) {
        final String resolvHost;
        if (!hostName.matches("^[a-zA-Z0-9\\-\\.]+$") && Application.getJavaVersion() >= Application.JAVA16) {
            resolvHost = java.net.IDN.toASCII(hostName.trim());
        } else {
            /* remove spaces....so literal IP's work without resolving */
            resolvHost = hostName.trim();
        }
        return resolvHost.toLowerCase(Locale.ENGLISH);
    }

    protected void setHostname(String hostName) {
        this.hostName = hostName;
    }

    protected boolean isHostnameResolved() {
        return this.hostName != null;
    }

    public void connect() throws IOException {
        boolean sslSNIWorkAround = false;
        connect: while (true) {
            if (this.isConnectionSocketValid()) {
                return;/* oder fehler */
            }
            this.resetConnection();
            if (!isHostnameResolved()) {
                setHostname(resolveHostname(this.httpURL.getHost()));
            }
            this.connectionSocket = this.getKeepAliveSocket();
            if (this.connectionSocket == null) {
                if (this.remoteIPs == null) {
                    this.remoteIPs = this.resolvHostIP(getHostname());
                }
                this.connectionSocket = this.getKeepAliveSocket();
            }
            if (this.connectionSocket == null) {
                /* try all different ip's until one is valid and connectable */
                IOException ee = null;
                for (final InetAddress host : this.remoteIPs) {
                    this.resetConnection();
                    int port = this.httpURL.getPort();
                    if (port == -1) {
                        port = this.httpURL.getDefaultPort();
                    }
                    long startTime = System.currentTimeMillis();
                    final HTTPProxy lProxy = getProxy();
                    InetAddress bindInetAddress = null;
                    if (lProxy != null) {
                        if (lProxy.isDirect()) {
                            bindInetAddress = getDirectInetAddress(lProxy);
                        } else if (!lProxy.isNone()) {
                            throw new ProxyConnectException("Invalid Direct Proxy", lProxy);
                        }
                    }
                    InetSocketAddress connectedInetSocketAddress = null;
                    try {
                        /* try to connect to given host now */
                        connectedInetSocketAddress = new InetSocketAddress(host, port);
                        int connectTimeout = this.getConnectTimeout();
                        if (connectTimeout == 0) {
                            startTime = System.currentTimeMillis();
                            this.connectionSocket = createConnectionSocket(bindInetAddress);
                            /** no workaround for infinite connect timeouts **/
                            this.connectionSocket.getSocket().connect(connectedInetSocketAddress, connectTimeout);
                        } else {
                            /**
                             * workaround for too early connect timeouts
                             */
                            while (true) {
                                startTime = System.currentTimeMillis();
                                this.connectionSocket = createConnectionSocket(bindInetAddress);
                                final long beforeConnect = System.currentTimeMillis();
                                try {
                                    this.connectionSocket.getSocket().connect(connectedInetSocketAddress, connectTimeout);
                                    break;
                                } catch (final ConnectException e) {
                                    closeConnectionSocket(this.connectionSocket);
                                    if (StringUtils.containsIgnoreCase(e.getMessage(), "timed out")) {
                                        int timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                        if (timeout < 1000) {
                                            System.out.println("Too Fast ConnectTimeout(Normal): " + timeout + "->Wait " + (2000 - timeout));
                                            try {
                                                Thread.sleep(2000 - timeout);
                                            } catch (final InterruptedException ie) {
                                                throw e;
                                            }
                                            timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                        }
                                        final int lastConnectTimeout = connectTimeout;
                                        connectTimeout = Math.max(0, connectTimeout - timeout);
                                        if (connectTimeout == 0 || Thread.currentThread().isInterrupted()) {
                                            throw e;
                                        }
                                        System.out.println("Workaround for ConnectTimeout(Normal): " + lastConnectTimeout + ">" + timeout);
                                    } else {
                                        throw e;
                                    }
                                } catch (final SocketTimeoutException e) {
                                    closeConnectionSocket(this.connectionSocket);
                                    if (StringUtils.containsIgnoreCase(e.getMessage(), "timed out")) {
                                        int timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                        if (timeout < 1000) {
                                            System.out.println("Too Fast ConnectTimeout(Interrupted): " + timeout + "->Wait " + (2000 - timeout));
                                            try {
                                                Thread.sleep(2000 - timeout);
                                            } catch (final InterruptedException ie) {
                                                throw e;
                                            }
                                            timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                        }
                                        final int lastConnectTimeout = connectTimeout;
                                        connectTimeout = Math.max(0, connectTimeout - timeout);
                                        if (connectTimeout == 0 || Thread.currentThread().isInterrupted()) {
                                            throw e;
                                        }
                                        System.out.println("Workaround for ConnectTimeout(Interrupted): " + lastConnectTimeout + ">" + timeout);
                                    } else {
                                        throw e;
                                    }
                                }
                            }
                        }
                        if (this.httpURL.getProtocol().startsWith("https")) {
                            final SSLSocketStreamFactory factory = getSSLSocketStreamFactory();
                            if (sslSNIWorkAround) {
                                /* wrong configured SNI at serverSide */
                                this.connectionSocket = factory.create(connectionSocket, "", port, true, isSSLTrustALL());
                            } else {
                                this.connectionSocket = factory.create(connectionSocket, getHostname(), port, true, isSSLTrustALL());
                            }
                        }
                        this.connectTime = System.currentTimeMillis() - startTime;
                        ee = null;
                        break;
                    } catch (final IOException e) {
                        this.disconnect();
                        this.connectExceptions.add(connectedInetSocketAddress + "|" + e.getMessage());
                        if (sslSNIWorkAround == false && (StringUtils.contains(e.getMessage(), "unrecognized_name"))) {
                            sslSNIWorkAround = true;
                            continue connect;
                        }
                        if (bindInetAddress != null) {
                            ee = new ProxyConnectException(e, lProxy);
                        } else {
                            ee = e;
                        }
                    }
                }
                if (ee != null) {
                    throw ee;
                }
            }
            this.setReadTimeout(this.readTimeout);
            /* now send Request */
            final Socket lastConnectionSocket = getConnectionSocket().getSocket();
            try {
                this.lastConnection = lastConnectionSocket.getInetAddress();
                this.lastConnectionPort = lastConnectionSocket.getPort();
                this.sendRequest();
                return;
            } catch (final javax.net.ssl.SSLException e) {
                try {
                    this.connectExceptions.add(lastConnectionSocket.getInetAddress() + "|" + e.getMessage());
                } finally {
                    this.disconnect();
                }
                if (sslSNIWorkAround == false && e.getMessage().contains("unrecognized_name")) {
                    sslSNIWorkAround = true;
                    continue connect;
                }
                throw e;
            } catch (final HTTPKeepAliveSocketException e) {
                if (KEEPALIVE.EXTERNAL_EXCEPTION.equals(this.getKeepAlive())) {
                    //
                    throw e;
                }
            }
        }
    }

    protected SSLSocketStreamFactory getSSLSocketStreamFactory() {
        return getDefaultSSLSocketStreamFactory();
    }

    protected boolean isKeepAlivedEnabled() {
        final KEEPALIVE keepAlive = this.getKeepAlive();
        if (!KEEPALIVE.DISABLED.equals(keepAlive)) {
            final String connectionRequest = this.getRequestProperty(HTTPConstants.HEADER_REQUEST_CONNECTION);
            final String connectionResponse = this.getHeaderField(HTTPConstants.HEADER_REQUEST_CONNECTION);
            final boolean tryKeepAlive = (!this.isRequiresOutputStream() || KEEPALIVE.EXTERNAL_EXCEPTION.equals(keepAlive)) && (connectionResponse == null || StringUtils.containsIgnoreCase(connectionResponse, "Keep-Alive")) && (connectionRequest == null || !StringUtils.containsIgnoreCase(connectionRequest, "close"));
            return tryKeepAlive;
        } else {
            return false;
        }
    }

    protected String fromBytes(byte[] bytes, int start, int end) throws IOException {
        final StringBuilder sb = new StringBuilder();
        if (start < 0) {
            start = 0;
        }
        if (end < 0 || end >= bytes.length) {
            end = bytes.length;
        }
        for (int index = start; index < end; index++) {
            final int c = bytes[index] & 0xff;
            if (c <= 127) {
                sb.append((char) c);
            } else {
                final String hexEncoded = Integer.toString(c, 16);
                if (hexEncoded.length() == 1) {
                    sb.append("%0");
                } else {
                    sb.append("%");
                }
                sb.append(hexEncoded);
            }
        }
        return sb.toString();
    }

    protected synchronized void connectInputStream() throws IOException {
        final SocketStreamInterface connectionSocket = this.getConnectionSocket();
        final boolean isKeepAliveSocket;
        synchronized (HTTPConnectionImpl.LOCK) {
            isKeepAliveSocket = connectionSocket != null && HTTPConnectionImpl.KEEPALIVESOCKETS.containsKey(connectionSocket);
        }
        try {
            if (this.isRequiresOutputStream() && this.postTodoLength >= 0) {
                final long done = ((CountingOutputStream) this.outputStream).transferedBytes();
                if (done != this.postTodoLength) {
                    throw new IllegalStateException("Content-Length " + this.postTodoLength + " does not match send " + done + " bytes");
                }
            }
            if (this.inputStreamConnected) {
                return;
            }
            if (this.isRequiresOutputStream()) {
                /* flush outputstream in case some buffers are not flushed yet */
                this.outputStream.flush();
            }
            final long startTime = System.currentTimeMillis();
            final InputStream inputStream = connectionSocket.getInputStream();
            this.inputStreamConnected = true;
            /* first read http header */
            ByteBuffer header = HTTPConnectionUtils.readheader(inputStream, true);
            if (header.limit() == 0) {
                throw new EOFException("empty HTTP-Response");
            }
            if (header.hasArray()) {
                this.httpHeader = new String(header.array(), 0, header.limit(), "ISO-8859-1").trim();
            } else {
                final byte[] bytes = new byte[header.limit()];
                header.get(bytes);
                this.httpHeader = new String(bytes, "ISO-8859-1").trim();
            }
            /* parse response code/message */
            if (this.httpHeader.matches("^[a-zA-Z0-9/\\.]+\\s*\\d+.*?")) {
                /**
                 * HTTP/1.0 or HTTP/1.1 or HTTP/1.0 compatible header
                 */
                final String code = new Regex(this.httpHeader, "[a-zA-Z0-9/\\.]+\\s*(\\d+)").getMatch(0);
                if (code != null) {
                    this.httpResponseCode = Integer.parseInt(code);
                }
                this.httpResponseMessage = new Regex(this.httpHeader, "[a-zA-Z0-9/\\.]+\\s*\\d+\\s*(.+)").getMatch(0);
                if (this.httpResponseMessage == null) {
                    this.httpResponseMessage = "";
                }
            } else {
                if (isKeepAliveSocket) {
                    throw new IOException("unknown HTTP response");
                }
                this.invalidHttpHeader = this.httpHeader;
                this.httpHeader = HTTPConnectionImpl.UNKNOWN_HTTP_RESPONSE;
                // Unknown HTTP Response: 999!
                this.httpResponseCode = 999;
                this.httpResponseMessage = HTTPConnectionImpl.UNKNOWN_HTTP_RESPONSE;
                if (header.limit() > 0) {
                    /*
                     * push back the data that got read because no http header exists
                     */
                    final PushbackInputStream pushBackInputStream;
                    if (header.hasArray()) {
                        pushBackInputStream = new PushbackInputStream(inputStream, header.limit());
                        pushBackInputStream.unread(header.array(), 0, header.limit());
                    } else {
                        final byte[] bytes = new byte[header.limit()];
                        header.get(bytes);
                        pushBackInputStream = new PushbackInputStream(inputStream, bytes.length);
                        pushBackInputStream.unread(bytes);
                    }
                    this.inputStream = pushBackInputStream;
                } else {
                    /* nothing to push back */
                    this.inputStream = inputStream;
                }
                return;
            }
            /* read rest of http headers */
            header = HTTPConnectionUtils.readheader(inputStream, false);
            final String temp;
            if (header.hasArray()) {
                temp = fromBytes(header.array(), 0, header.limit());
            } else {
                final byte[] bytes = new byte[header.limit()];
                header.get(bytes);
                temp = fromBytes(bytes, -1, -1);
            }
            this.requestTime = System.currentTimeMillis() - startTime;
            /*
             * split header into single strings, use RN or N(buggy fucking non rfc)
             */
            String[] headerStrings = temp.split("(\r\n)|(\n)");
            for (final String line : headerStrings) {
                String key = null;
                String value = null;
                int index = 0;
                if ((index = line.indexOf(": ")) > 0) {
                    key = line.substring(0, index);
                    value = line.substring(index + 2);
                } else if ((index = line.indexOf(":")) > 0) {
                    /* buggy servers that don't have :space ARG */
                    key = line.substring(0, index);
                    value = line.substring(index + 1);
                } else {
                    key = null;
                    value = line;
                }
                if (key != null) {
                    key = key.trim();
                }
                if (value != null) {
                    value = value.trim();
                }
                List<String> list = this.headers.get(key);
                if (list == null) {
                    list = new ArrayList<String>();
                    this.headers.put(key, list);
                }
                list.add(value);
            }
            headerStrings = null;
            InputStream wrappedInputStream;
            if (this.isKeepAlivedEnabled()) {
                /* keep-alive-> do not close the inputstream! */
                wrappedInputStream = new FilterInputStream(inputStream) {
                    @Override
                    public void close() throws IOException {
                        /* do not close, keep-Alive */
                    }
                };
            } else {
                wrappedInputStream = inputStream;
            }
            if (RequestMethod.HEAD.equals(this.getRequestMethod())) {
                wrappedInputStream = new LimitedInputStream(wrappedInputStream, 0);
            } else {
                final boolean isChunked = StringUtils.containsIgnoreCase(this.getHeaderField(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING), HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED);
                if (isChunked) {
                    /* wrap chunkedInputStream */
                    wrappedInputStream = new ChunkedInputStream(wrappedInputStream);
                } else {
                    final long contentLength = this.getContentLength();
                    if (contentLength >= 0) {
                        /* wrap limitedInputStream */
                        wrappedInputStream = new LimitedInputStream(wrappedInputStream, contentLength);
                    }
                }
            }
            this.inputStream = wrappedInputStream;
        } catch (final HTTPKeepAliveSocketException e) {
            this.disconnect();
            throw e;
        } catch (final IOException e) {
            this.disconnect();
            if (connectionSocket != null) {
                synchronized (HTTPConnectionImpl.LOCK) {
                    if (HTTPConnectionImpl.KEEPALIVESOCKETS.remove(connectionSocket) != null) {
                        throw new HTTPKeepAliveSocketException(e, connectionSocket.getSocket());
                    }
                }
            }
            throw e;
        }
    }

    public void disconnect() {
        SocketStreamInterface connectionSocket = null;
        try {
            connectionSocket = this.getConnectionSocket();
            if (connectionSocket != null && !this.putKeepAliveSocket(connectionSocket)) {
                connectionSocket.close();
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            try {
                if (connectionSocket != null) {
                    connectionSocket.close();
                }
            } catch (final Throwable ignore) {
            }
        } finally {
            this.connectionSocket = null;
        }
    }

    @Override
    public void finalizeConnect() throws IOException {
        this.connect();
        this.connectInputStream();
    }

    @Override
    public int[] getAllowedResponseCodes() {
        return this.allowedResponseCodes;
    }

    public String getCharset() {
        if (this.customcharset != null) {
            return this.customcharset;
        }
        String charSet = this.getContentType();
        if (charSet != null) {
            final int charSetIndex = this.getContentType().toLowerCase().indexOf("charset=");
            if (charSetIndex > 0) {
                charSet = this.getContentType().substring(charSetIndex + 8).trim();
                if (charSet.length() > 2) {
                    if (charSet.startsWith("\"")) {
                        charSet = charSet.substring(1);
                        final int indexLast = charSet.lastIndexOf("\"");
                        if (indexLast > 0) {
                            charSet = charSet.substring(0, indexLast);
                        }
                    }
                    return charSet;
                }
            }
        }
        return null;
    }

    @Override
    public long getCompleteContentLength() {
        final long[] ranges = this.getRange();
        if (ranges != null && ranges[2] >= 0) {
            return ranges[2];
        } else {
            return this.getContentLength();
        }
    }

    public long getContentLength() {
        final String length = this.getHeaderField("Content-Length");
        if (length != null) {
            return Long.parseLong(length.trim());
        } else {
            return -1;
        }
    }

    public String getContentType() {
        final String type = this.getHeaderField("Content-Type");
        if (type == null) {
            return "unknown";
        } else {
            return type;
        }
    }

    public String getHeaderField(final String string) {
        final List<String> ret = this.headers.get(string);
        if (ret == null || ret.size() == 0) {
            return null;
        } else {
            return ret.get(0);
        }
    }

    public Map<String, List<String>> getHeaderFields() {
        return this.headers;
    }

    public List<String> getHeaderFields(final String string) {
        final List<String> ret = this.headers.get(string);
        if (ret == null || ret.size() == 0) {
            return null;
        }
        return ret;
    }

    public InputStream getInputStream() throws IOException {
        this.connect();
        this.connectInputStream();
        final int code = this.getResponseCode();
        if (this.isOK() || code == 404 || code == 403 || code == 416 || code == 401) {
            if (this.convertedInputStream != null) {
                return this.convertedInputStream;
            }
            if (this.contentDecoded && !RequestMethod.HEAD.equals(this.getRequestMethod())) {
                final String encodingTransfer = this.getHeaderField("Content-Transfer-Encoding");
                if ("base64".equalsIgnoreCase(encodingTransfer)) {
                    /* base64 encoded content */
                    this.inputStream = new Base64InputStream(this.inputStream);
                }
                /* we convert different content-encodings to normal inputstream */
                final String encoding = this.getHeaderField("Content-Encoding");
                if (encoding == null || encoding.length() == 0 || "none".equalsIgnoreCase(encoding)) {
                    /* no encoding */
                    this.convertedInputStream = this.inputStream;
                } else if ("gzip".equalsIgnoreCase(encoding)) {
                    /* gzip encoding */
                    this.convertedInputStream = new GZIPInputStream(this.inputStream);
                } else if ("deflate".equalsIgnoreCase(encoding)) {
                    /* deflate encoding */
                    this.convertedInputStream = new java.util.zip.InflaterInputStream(this.inputStream, new java.util.zip.Inflater(true));
                } else {
                    /* unsupported */
                    this.contentDecoded = false;
                    this.convertedInputStream = this.inputStream;
                }
            } else {
                /*
                 * use original inputstream OR LimitedInputStream from HeadRequest
                 */
                this.convertedInputStream = this.inputStream;
            }
            return this.convertedInputStream;
        } else {
            throw new IOException(this.getResponseCode() + " " + this.getResponseMessage());
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (this.outputStream != null && this.isRequiresOutputStream()) {
            return this.outputStream;
        }
        throw new IOException("OutputStream is not available");
    }

    public long[] getRange() {
        if (this.ranges != null) {
            return this.ranges;
        }
        final String contentRange = this.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_RANGE);
        if (contentRange != null) {
            String[] range = null;
            // the total size can be given by a * Like: bytes 26395608-29695059/*
            if ((range = new Regex(contentRange, "\\s*(\\d+)\\s*-\\s*(\\d+)\\s*/\\s*(\\d+|\\*)").getRow(0)) != null) {
                /* RFC-2616 */
                /* START-STOP/SIZE */
                /* Content-Range=[133333332-199999999/200000000] */
                final long gotSB = Long.parseLong(range[0]);
                final long gotEB = Long.parseLong(range[1]);
                final long gotS = "*".equals(range[2]) ? -1 : Long.parseLong(range[2]);
                this.ranges = new long[] { gotSB, gotEB, gotS };
            } else if ((range = new Regex(contentRange, "\\s*(\\d+)\\s*-\\s*/\\s*(\\d+|\\*)").getRow(0)) != null && this.getResponseCode() != 416) {
                /* only parse this when we have NO 416 (invalid range request) */
                /* NON RFC-2616! STOP is missing */
                /*
                 * this happend for some stupid servers, seems to happen when request is bytes=9500- (x till end)
                 */
                /* START-/SIZE */
                /* content-range: bytes 1020054729-/1073741824 */
                final long gotSB = Long.parseLong(range[0]);
                if ("*".equals(range[1])) {
                    this.ranges = new long[] { gotSB, -1, -1 };
                } else {
                    final long gotS = Long.parseLong(range[1]);
                    this.ranges = new long[] { gotSB, gotS - 1, gotS };
                }
            } else if (this.getResponseCode() == 416 && (range = new Regex(contentRange, ".\\s*\\*\\s*/\\s*(\\d+|\\*)").getRow(0)) != null) {
                /* a 416 may respond with content-range * | content.size answer */
                this.ranges = new long[] { -1, -1, "*".equals(range[0]) ? -1 : Long.parseLong(range[0]) };
            } else if (this.getResponseCode() == 206 && (range = new Regex(contentRange, "[ \\*]+/(\\d+)").getRow(0)) != null) {
                /* RFC-2616 */
                /* a nginx 206 may respond with */
                /* content-range: bytes * / 554407633 */
                /*
                 * A response with status code 206 (Partial Content) MUST NOT include a Content-Range field with a byte-range- resp-spec of
                 * "*".
                 */
                this.ranges = new long[] { -1, Long.parseLong(range[0]), Long.parseLong(range[0]) };
            } else {
                /* unknown range header format! */
                System.out.println(contentRange + " format is unknown!");
            }
        }
        return this.ranges;
    }

    protected String getHostname() {
        if (isHostnameResolved()) {
            return this.hostName;
        } else {
            return resolveHostname(httpURL.getHost());
        }
    }

    protected String getRequestInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("----------------Request Information-------------\r\n");
        sb.append("URL: ").append(this.getURL()).append("\r\n");
        final SocketStreamInterface socketStream = getConnectionSocket();
        final Socket lhttpSocket;
        if (socketStream != null) {
            lhttpSocket = socketStream.getSocket();
        } else {
            lhttpSocket = null;
        }
        final InetAddress lLastConnection = this.lastConnection;
        if (lhttpSocket != null && lhttpSocket.isConnected()) {
            if (socketStream instanceof SSLSocketStreamInterface) {
                final SSLSocketStreamInterface sslSocketStream = (SSLSocketStreamInterface) socketStream;
                sb.append("SSLCipher: ").append(sslSocketStream.getCipherSuite()).append("\r\n");
            }
            sb.append("ConnectIP: ").append(lhttpSocket.getInetAddress()).append(":").append(lhttpSocket.getPort()).append("\r\n");
        } else if (lLastConnection != null) {
            sb.append("ConnectIP: ").append(lLastConnection).append(":").append(this.lastConnectionPort).append("\r\n");
        } else {
            sb.append("Host: ").append(getHostname()).append("\r\n");
        }
        if (this.proxy != null && this.proxy.isDirect()) {
            if (lhttpSocket != null) {
                sb.append("Local: ").append(this.proxy.getLocal()).append(lhttpSocket.getLocalAddress().toString()).append("\r\n");
            } else {
                sb.append("Local: ").append(this.proxy.getLocal()).append("\r\n");
            }
        }
        sb.append("Connection-Timeout: ").append(this.connectTimeout + "ms").append("\r\n");
        sb.append("Read-Timeout: ").append(readTimeout + "ms").append("\r\n");
        if (this.connectExceptions.size() > 0) {
            sb.append("----------------ConnectionExceptions-------------------------\r\n");
            int index = 0;
            for (String connectException : this.connectExceptions) {
                sb.append(index++).append(":").append(connectException).append("\r\n");
            }
        }
        sb.append("----------------Request-------------------------\r\n");
        if (this.inputStream != null) {
            sb.append(this.httpMethod.toString()).append(' ').append(this.httpPath).append(" HTTP/1.1\r\n");
            final Iterator<Entry<String, String>> it = this.getRequestProperties().entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, String> next = it.next();
                if (next.getValue() == null) {
                    continue;
                }
                sb.append(next.getKey());
                sb.append(": ");
                sb.append(next.getValue());
                sb.append("\r\n");
            }
        } else {
            sb.append("-------------Not Connected Yet!-----------------\r\n");
        }
        return sb.toString();
    }

    public RequestMethod getRequestMethod() {
        return this.httpMethod;
    }

    public Map<String, String> getRequestProperties() {
        return this.requestProperties;
    }

    public String getRequestProperty(final String string) {
        return this.requestProperties.get(string);
    }

    public long getRequestTime() {
        return this.requestTime;
    }

    public long getConnectTime() {
        return this.connectTime;
    }

    public int getResponseCode() {
        return this.httpResponseCode;
    }

    protected String getResponseInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("----------------Response Information------------\r\n");
        try {
            if (this.inputStream != null) {
                final long lconnectTime = getConnectTime();
                if (lconnectTime >= 0) {
                    sb.append("Connection-Time: ").append(lconnectTime + "ms").append("\r\n");
                } else {
                    sb.append("Connection-Time: keep-Alive\r\n");
                }
                final long lrequestTime = getRequestTime();
                sb.append("Request-Time: ").append(Math.max(0, lrequestTime) + "ms").append("\r\n");
                sb.append("----------------Response------------------------\r\n");
                this.connectInputStream();
                sb.append(this.httpHeader).append("\r\n");
                if (this.invalidHttpHeader != null) {
                    sb.append("InvalidHTTPHeader: ").append(this.invalidHttpHeader).append("\r\n");
                }
                for (final Entry<String, List<String>> next : this.getHeaderFields().entrySet()) {
                    for (int i = 0; i < next.getValue().size(); i++) {
                        if (next.getKey() == null) {
                            sb.append(next.getValue().get(i));
                            sb.append("\r\n");
                        } else {
                            sb.append(next.getKey());
                            sb.append(": ");
                            sb.append(next.getValue().get(i));
                            sb.append("\r\n");
                        }
                    }
                }
                sb.append("------------------------------------------------\r\n");
            } else {
                sb.append("-------------Not Connected Yet!------------------\r\n");
            }
        } catch (final IOException nothing) {
            sb.append("----------No InputStream Available!--------------\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public String getResponseMessage() {
        return this.httpResponseMessage;
    }

    public URL getURL() {
        return this.httpURL;
    }

    public boolean isConnected() {
        final SocketStreamInterface connectionSocket = getConnectionSocket();
        final Socket socket;
        if (connectionSocket != null && (socket = connectionSocket.getSocket()) != null && socket.isConnected()) {
            return true;
        }
        return false;
    }

    protected boolean isConnectionSocketValid() {
        final SocketStreamInterface connectionSocket = this.getConnectionSocket();
        final Socket socket;
        if (connectionSocket != null && (socket = connectionSocket.getSocket()) != null && socket.isConnected() && !socket.isClosed()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isContentDecoded() {
        return this.contentDecoded;
    }

    public boolean isContentDisposition() {
        return this.getHeaderField("Content-Disposition") != null;
    }

    public boolean isOK() {
        final int code = this.getResponseCode();
        if (code >= 200 && code < 400) {
            return true;
        }
        if (this.isResponseCodeAllowed(code)) {
            return true;
        }
        return false;
    }

    protected boolean isResponseCodeAllowed(final int code) {
        for (final int c : this.allowedResponseCodes) {
            if (c == code || c == -1) {
                return true;
            }
        }
        return false;
    }

    protected void putHostToTop(final Map<String, String> oldRequestProperties) {
        final HTTPHeaderMap<String> newRet = new HTTPHeaderMap<String>();
        final String host = oldRequestProperties.remove("Host");
        if (host != null) {
            newRet.put("Host", host);
        }
        newRet.putAll(oldRequestProperties);
        oldRequestProperties.clear();
        oldRequestProperties.putAll(newRet);
    }

    protected boolean isRequiresOutputStream() {
        return httpMethod.requiresOutputStream;
    }

    public InetAddress[] resolvHostIP(final String host) throws IOException {
        final InetAddress[] ips = HTTPConnectionUtils.resolvHostIP(host);
        if (ips != null) {
            final List<InetAddress> ipv4FirstList = new ArrayList<InetAddress>();
            for (final InetAddress ip : ips) {
                if (ip instanceof Inet4Address) {
                    ipv4FirstList.add(ip);
                }
            }
            for (final InetAddress ip : ips) {
                if (ip instanceof Inet6Address) {
                    ipv4FirstList.add(ip);
                }
            }
            return ipv4FirstList.toArray(new InetAddress[0]);
        }
        return null;
    }

    protected void sendRequest() throws UnsupportedEncodingException, IOException {
        /* now send Request */
        final SocketStreamInterface connectionSocket = getConnectionSocket();
        final boolean isKeepAliveSocket;
        synchronized (HTTPConnectionImpl.LOCK) {
            isKeepAliveSocket = connectionSocket != null && HTTPConnectionImpl.KEEPALIVESOCKETS.containsKey(connectionSocket);
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(this.httpMethod.name()).append(' ').append(this.httpPath).append(" HTTP/1.1\r\n");
        boolean hostSet = false;
        /* check if host entry does exist */
        for (final String key : this.requestProperties.keySet()) {
            if ("Host".equalsIgnoreCase(key)) {
                hostSet = true;
                break;
            }
        }
        if (hostSet == false) {
            /* host entry does not exist,lets add it */
            this.addHostHeader();
        }
        this.putHostToTop(this.requestProperties);
        final Iterator<Entry<String, String>> it = this.requestProperties.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, String> next = it.next();
            if (next.getValue() == null) {
                continue;
            }
            if ("Content-Length".equalsIgnoreCase(next.getKey())) {
                /* content length to check if we send out all data */
                this.postTodoLength = Long.parseLong(next.getValue().trim());
            }
            sb.append(next.getKey()).append(": ").append(next.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        final Socket socket = connectionSocket.getSocket();
        try {
            final OutputStream outputStream = connectionSocket.getOutputStream();
            outputStream.write(sb.toString().getBytes("ISO-8859-1"));
            outputStream.flush();
            if (this.isRequiresOutputStream()) {
                this.outputStream = new CountingOutputStream(outputStream) {
                    @Override
                    public void close() throws IOException {
                        if (!isKeepAliveSocket) {
                            super.close();
                        }
                    }

                    private final void throwIOException(final IOException e) throws IOException {
                        if (isKeepAliveSocket) {
                            synchronized (HTTPConnectionImpl.LOCK) {
                                HTTPConnectionImpl.KEEPALIVESOCKETS.remove(connectionSocket);
                            }
                            throw new HTTPKeepAliveSocketException(e, socket);
                        } else {
                            throw e;
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        try {
                            super.flush();
                        } catch (final IOException e) {
                            throwIOException(e);
                        }
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        try {
                            super.write(b);
                        } catch (final IOException e) {
                            throwIOException(e);
                        }
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        try {
                            super.write(b, off, len);
                        } catch (final IOException e) {
                            throwIOException(e);
                        }
                    }

                    @Override
                    public void write(int b) throws IOException {
                        try {
                            super.write(b);
                        } catch (final IOException e) {
                            throwIOException(e);
                        }
                    }
                };
            } else {
                this.connectInputStream();
            }
        } catch (final HTTPKeepAliveSocketException e) {
            this.disconnect();
            throw e;
        } catch (final IOException e) {
            this.disconnect();
            if (connectionSocket != null) {
                synchronized (HTTPConnectionImpl.LOCK) {
                    if (HTTPConnectionImpl.KEEPALIVESOCKETS.remove(connectionSocket) != null) {
                        throw new HTTPKeepAliveSocketException(e, connectionSocket.getSocket());
                    }
                }
            }
            throw e;
        }
    }

    @Override
    public void setAllowedResponseCodes(final int[] codes) {
        if (codes == null) {
            throw new IllegalArgumentException("codes==null");
        }
        this.allowedResponseCodes = codes;
    }

    public void setCharset(final String Charset) {
        this.customcharset = Charset;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = Math.max(0, connectTimeout);
    }

    @Override
    public void setContentDecoded(final boolean b) {
        if (this.convertedInputStream != null) {
            throw new IllegalStateException("InputStream already in use!");
        }
        this.contentDecoded = b;
    }

    public void setReadTimeout(final int readTimeout) {
        try {
            this.readTimeout = Math.max(0, readTimeout);
            final SocketStreamInterface connectionSocket = this.getConnectionSocket();
            final Socket socket;
            if (connectionSocket != null && (socket = connectionSocket.getSocket()) != null) {
                socket.setSoTimeout(readTimeout);
            }
        } catch (final Throwable ignore) {
        }
    }

    public void setRequestMethod(final RequestMethod method) {
        this.httpMethod = method;
    }

    public void setRequestProperty(final String key, final String value) {
        this.requestProperties.put(key, value);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getRequestInfo());
        sb.append(this.getResponseInfo());
        return sb.toString();
    }

    @Override
    public void setSSLTrustALL(boolean trustALL) {
        this.sslTrustALL = trustALL;
    }

    @Override
    public boolean isSSLTrustALL() {
        return this.sslTrustALL;
    }
}
