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
package org.appwork.utils.net.httpserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.HTTPBridge;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

/**
 * @author daniel
 *
 */
public class HttpServer implements Runnable, HTTPBridge {
    private final int                                      wishPort;
    private final AtomicReference<ServerSocket>            controlSocket   = new AtomicReference<ServerSocket>(null);
    private volatile Thread                                serverThread    = null;
    private boolean                                        localhostOnly   = false;
    private boolean                                        debug           = false;
    private final CopyOnWriteArrayList<HttpRequestHandler> requestHandlers = new CopyOnWriteArrayList<HttpRequestHandler>();

    public HttpServer(final int port) {
        this.wishPort = port;
    }

    protected Runnable createConnectionHandler(final Socket clientSocket) throws IOException {
        return new HttpConnection(this, clientSocket);
    }

    public List<HttpRequestHandler> getHandler() {
        return this.requestHandlers;
    }

    protected InetAddress getLocalHost() {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getByName("127.0.0.1");
        } catch (final UnknownHostException e1) {
        }
        if (localhost != null) {
            return localhost;
        }
        try {
            localhost = InetAddress.getByName(null);
        } catch (final UnknownHostException e1) {
        }
        return localhost;
    }

    /**
     * @return the port
     */
    public int getPort() {
        try {
            final ServerSocket lControlSocket = controlSocket.get();
            if (lControlSocket != null) {
                return lControlSocket.getLocalPort();
            }
        } catch (final Throwable e) {
        }
        return this.getWishedPort();
    }

    public int getWishedPort() {
        return wishPort;
    }

    /**
     * @return the debug
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * @return the localhostOnly
     */
    public boolean isLocalhostOnly() {
        return this.localhostOnly;
    }

    public boolean isRunning() {
        return controlSocket.get() != null && this.serverThread != null;
    }

    /*
     * to register a new handler we create a copy of current handlerList and then add new handler to it and set it as new handlerList. by
     * doing so, all current connections dont have to sync on their handlerlist
     */
    public HttpHandlerInfo registerRequestHandler(final HttpRequestHandler handler) {
        if (handler != null) {
            requestHandlers.addIfAbsent(handler);
        }
        return new HttpHandlerInfo(this, handler);
    }

    public void run() {
        final ServerSocket socket = this.controlSocket.get();
        try {
            socket.setSoTimeout(5 * 60 * 1000);
        } catch (final SocketException e1) {
            e1.printStackTrace();
        }
        ThreadPoolExecutor threadPool = null;
        try {
            threadPool = new ThreadPoolExecutor(0, 20, 10000l, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100), new ThreadFactory() {
                public Thread newThread(final Runnable r) {
                    return new HttpConnectionThread(HttpServer.this, r);
                }
            }, new ThreadPoolExecutor.AbortPolicy()) {
                final ThreadPoolExecutor threadPool;
                {
                    threadPool = this;
                }

                @Override
                protected void beforeExecute(final Thread t, final Runnable r) {
                    /*
                     * WORKAROUND for stupid SUN /ORACLE way of "how a threadpool should work" !
                     */
                    final int active = threadPool.getPoolSize();
                    final int max = threadPool.getMaximumPoolSize();
                    if (active < max) {
                        final int working = threadPool.getActiveCount();
                        if (working == active) {
                            /*
                             * we can increase max pool size so new threads get started
                             */
                            threadPool.setCorePoolSize(Math.min(max, active + 1));
                        }
                    }
                    if (t instanceof HttpConnectionThread && r instanceof HttpConnection) {
                        ((HttpConnectionThread) t).setCurrentConnection((HttpConnection) r);
                    }
                    super.beforeExecute(t, r);
                }
            };
            threadPool.allowCoreThreadTimeOut(true);
            while (controlSocket.get() == socket) {
                try {
                    final Socket clientSocket = socket.accept();
                    try {
                        threadPool.execute(this.createConnectionHandler(clientSocket));
                    } catch (final IOException e) {
                        e.printStackTrace();
                        try {
                            clientSocket.close();
                        } catch (final Throwable e2) {
                        }
                    } catch (final RejectedExecutionException e) {
                        e.printStackTrace();
                        try {
                            clientSocket.close();
                        } catch (final Throwable e2) {
                        }
                    }
                } catch (final SocketTimeoutException e) {
                    /*
                     * nothing, our 5 mins connect timeout for the http server socket
                     */
                } catch (final IOException e) {
                    break;
                }
            }
        } finally {
            this.controlSocket.compareAndSet(socket, null);
            try {
                socket.close();
            } catch (final Throwable e) {
            }
            if (threadPool != null) {
                final List<Runnable> waiting = threadPool.shutdownNow();
                if (waiting != null) {
                    /* close all waiting HttpConnections */
                    for (final Runnable runnable : waiting) {
                        try {
                            if (runnable instanceof HttpConnection) {
                                ((HttpConnection) runnable).closeConnection();
                            }
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    /**
     * @param debug
     *            the debug to set
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    /**
     * @param localhostOnly
     *            the localhostOnly to set
     */
    public void setLocalhostOnly(final boolean localhostOnly) {
        this.localhostOnly = localhostOnly;
    }

    public synchronized void shutdown() {
        try {
            final ServerSocket lControlSocket = controlSocket.getAndSet(null);
            if (lControlSocket != null) {
                lControlSocket.close();
            }
        } catch (final Throwable e) {
        }
    }

    private int lastPort = -1;

    public synchronized void start() throws IOException {
        final ServerSocket controlSocket;
        final int port;
        if (lastPort != -1) {
            port = lastPort;
        } else {
            port = getWishedPort();
        }
        if (this.isLocalhostOnly()) {
            /* we only want localhost bound here */
            final SocketAddress socketAddress = new InetSocketAddress(this.getLocalHost(), port);
            controlSocket = new ServerSocket();
            controlSocket.setReuseAddress(true);
            controlSocket.bind(socketAddress);
        } else {
            controlSocket = new ServerSocket(port);
            controlSocket.setReuseAddress(true);
        }
        try {
            final ServerSocket oldControlSocket = this.controlSocket.getAndSet(controlSocket);
            if (oldControlSocket != null) {
                oldControlSocket.close();
            }
        } catch (final Throwable e) {
        }
        lastPort = controlSocket.getLocalPort();
        final Thread serverThread = new Thread(this);
        serverThread.setName("HttpServerThread|Port:" + getWishedPort() + "->" + getPort() + "|LocalHost:" + this.localhostOnly);
        this.serverThread = serverThread;
        serverThread.start();
    }

    public synchronized void stop() {
        try {
            final ServerSocket lControlSocket = controlSocket.getAndSet(null);
            if (lControlSocket != null) {
                lControlSocket.close();
            }
        } catch (final Throwable e) {
        }
        lastPort = -1;
    }

    /*
     * to unregister a new handler we create a copy of current handlerList and then remove handler to it and set it as new handlerList. by
     * doing so, all current connections dont have to sync on their handlerlist
     */
    public void unregisterRequestHandler(final HttpRequestHandler handler) {
        if (handler != null) {
            requestHandlers.remove(handler);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.net.httpserver.requests.HTTPBridge#canHandleChunkedEncoding(org.appwork.utils.net.httpserver.requests.HttpRequest,
     * org.appwork.utils.net.httpserver.responses.HttpResponse)
     */
    @Override
    public boolean canHandleChunkedEncoding(HttpRequest request, HttpResponse response) {
        return true;
    }
}
