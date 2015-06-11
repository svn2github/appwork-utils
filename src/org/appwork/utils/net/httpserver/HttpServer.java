/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpserver
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
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

/**
 * @author daniel
 *
 */
public class HttpServer implements Runnable {

    private final int                                      port;
    private final AtomicReference<ServerSocket>            controlSocket   = new AtomicReference<ServerSocket>(null);
    private volatile Thread                                serverThread    = null;
    private boolean                                        localhostOnly   = false;
    private boolean                                        debug           = false;
    private final CopyOnWriteArrayList<HttpRequestHandler> requestHandlers = new CopyOnWriteArrayList<HttpRequestHandler>();

    public HttpServer(final int port) {
        this.port = port;
    }

    protected HttpConnection createConnectionInstance(final Socket clientSocket) throws IOException {
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
        return this.port;
    }

    public int getWishedPort() {
        return port;
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
                        threadPool.execute(this.createConnectionInstance(clientSocket));
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

    public synchronized void start() throws IOException {
        final ServerSocket controlSocket;
        if (this.isLocalhostOnly()) {
            /* we only want localhost bound here */
            final SocketAddress socketAddress = new InetSocketAddress(this.getLocalHost(), this.port);
            controlSocket = new ServerSocket();
            controlSocket.setReuseAddress(true);
            controlSocket.bind(socketAddress);
        } else {
            controlSocket = new ServerSocket(this.port);
            controlSocket.setReuseAddress(true);
        }
        try {
            final ServerSocket oldControlSocket = this.controlSocket.getAndSet(controlSocket);
            if (oldControlSocket != null) {
                oldControlSocket.close();
            }
        } catch (final Throwable e) {
        }
        final Thread serverThread = new Thread(this);
        serverThread.setName("HttpServerThread:" + this.port + ":" + this.localhostOnly);
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

}
