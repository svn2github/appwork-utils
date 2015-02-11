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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyConnectException;

/**
 * @author daniel
 *
 */
public abstract class ProxySocket extends Socket {

    protected static int ensureRead(final InputStream is) throws IOException {
        final int read = is.read();
        if (read == -1) { throw new EOFException(); }
        return read;
    }

    protected static byte[] ensureRead(final InputStream is, final int size, final byte[] buffer) throws IOException {
        if (size <= 0) { throw new IllegalArgumentException("size <=0"); }
        final byte[] buf;
        if (buffer == null) {
            buf = new byte[size];
        } else {
            buf = buffer;
        }
        if (size > buf.length) { throw new IOException("buffer too small"); }
        int done = 0;
        int read = 0;
        while (done < size && (read = is.read(buf, done, size - done)) != -1) {
            done += read;
        }
        if (done != size) { throw new EOFException(); }
        return buf;
    }

    private SocketAddress   bindPoint         = null;

    private Boolean         keepAlive         = null;

    private Boolean         oobInline         = null;

    private final HTTPProxy proxy;

    private Socket          proxySocket       = null;

    private Integer         receiveBufferSize = null;

    private Boolean         reuseAddress      = null;
    private Integer         sendBufferSize    = null;

    private Integer         soLinger          = null;

    private Integer         soTimeout         = null;

    private Boolean         tcpNoDelay        = null;

    private Integer         trafficClass      = null;

    public ProxySocket(HTTPProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        if (this.proxySocket != null) {
            this.proxySocket.bind(bindpoint);
        } else {
            this.bindPoint = bindpoint;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.proxySocket != null) {
            this.proxySocket.close();
        }
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        this.connect(endpoint, 0);
    }

    @Override
    public void connect(SocketAddress endpoint, int connectTimeout) throws IOException {
        final InetAddress[] proxyHosts = HTTPConnectionUtils.resolvHostIP(this.getProxy().getHost());
        Socket connectSocket = null;
        try {
            IOException ioE = null;
            for (final InetAddress proxyHost : proxyHosts) {
                final InetSocketAddress proxySocketAddress = new InetSocketAddress(proxyHost, this.proxy.getPort());
                connectSocket = new Socket(Proxy.NO_PROXY);
                this.setSocketOptions(connectSocket);
                connectSocket.setSoTimeout(connectTimeout);
                try {
                    connectSocket.connect(proxySocketAddress, connectTimeout);
                    ioE = null;
                    break;
                } catch (final IOException e) {
                    connectSocket.close();
                    ioE = e;
                }
            }
            if (ioE != null) { throw new ProxyConnectException(ioE, this.proxy); }
            final Socket connectedSocket = this.connectProxySocket(connectSocket, endpoint);
            if (connectedSocket != null) {
                this.proxySocket = connectedSocket;
                return;
            }
            throw new ProxyConnectException(this.proxy);
        } catch (final IOException e) {
            if (e instanceof ProxyConnectException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        } finally {
            if (this.proxySocket == null && connectSocket != null) {
                connectSocket.close();
            }
        }
    }

    protected abstract Socket connectProxySocket(Socket proxySocket, SocketAddress endpoint) throws IOException;

    @Override
    public SocketChannel getChannel() {
        if (this.proxySocket != null) { return this.proxySocket.getChannel(); }
        return null;
    }

    @Override
    public InetAddress getInetAddress() {
        if (this.proxySocket != null) { return this.proxySocket.getInetAddress(); }
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (this.proxySocket != null) { return this.proxySocket.getInputStream(); }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getKeepAlive(); }
        if (this.keepAlive != null) { return this.keepAlive; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public InetAddress getLocalAddress() {
        if (this.proxySocket != null) { return this.proxySocket.getLocalAddress(); }
        return new InetSocketAddress(0).getAddress();
    }

    @Override
    public int getLocalPort() {
        if (this.proxySocket != null) { return this.proxySocket.getLocalPort(); }
        return -1;
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        if (this.proxySocket != null) { return this.proxySocket.getLocalSocketAddress(); }
        return null;
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getOOBInline(); }
        if (this.oobInline != null) { return this.oobInline; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (this.proxySocket != null) { return this.proxySocket.getOutputStream(); }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public int getPort() {
        if (this.proxySocket != null) { return this.proxySocket.getPort(); }
        return -1;
    }

    public HTTPProxy getProxy() {
        return this.proxy;
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getReceiveBufferSize(); }
        if (this.receiveBufferSize != null) { return this.receiveBufferSize; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        if (this.proxySocket != null) { return this.proxySocket.getRemoteSocketAddress(); }
        return null;
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getReuseAddress(); }
        if (this.reuseAddress != null) { return this.reuseAddress; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getSendBufferSize(); }
        if (this.sendBufferSize != null) { return this.sendBufferSize; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public int getSoLinger() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getSoLinger(); }
        return this.soLinger == null ? -1 : this.soLinger;
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getSoTimeout(); }
        if (this.soTimeout != null) { return this.soTimeout; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getTcpNoDelay(); }
        if (this.tcpNoDelay != null) { return this.tcpNoDelay; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public int getTrafficClass() throws SocketException {
        if (this.proxySocket != null) { return this.proxySocket.getTrafficClass(); }
        if (this.trafficClass != null) { return this.trafficClass; }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public boolean isBound() {
        if (this.proxySocket != null) { return this.proxySocket.isBound(); }
        return this.bindPoint != null;
    }

    @Override
    public boolean isClosed() {
        if (this.proxySocket != null) { return this.proxySocket.isClosed(); }
        return false;
    }

    @Override
    public boolean isConnected() {
        if (this.proxySocket != null) { return this.proxySocket.isClosed(); }
        return false;
    }

    @Override
    public boolean isInputShutdown() {
        if (this.proxySocket != null) { return this.proxySocket.isInputShutdown(); }
        return false;
    }

    @Override
    public boolean isOutputShutdown() {
        if (this.proxySocket != null) { return this.proxySocket.isOutputShutdown(); }
        return false;
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        if (this.proxySocket != null) {
            this.proxySocket.sendUrgentData(data);
        }
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setKeepAlive(on);
        } else {
            this.keepAlive = on;
        }
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setOOBInline(on);
        } else {
            this.oobInline = on;
        }
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        if (this.proxySocket != null) {
            this.proxySocket.setPerformancePreferences(connectionTime, latency, bandwidth);
        }
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setReceiveBufferSize(size);
        } else {
            this.receiveBufferSize = size;
        }
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setReuseAddress(on);
        } else {
            this.reuseAddress = on;
        }
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setSendBufferSize(size);
        } else {
            this.sendBufferSize = size;
        }
    }

    private void setSocketOptions(final Socket connectSocket) throws IOException {
        if (connectSocket != null) {
            if (this.bindPoint != null) {
                connectSocket.bind(this.bindPoint);
            }
            if (this.keepAlive != null) {
                connectSocket.setKeepAlive(this.keepAlive);
            }
            if (this.receiveBufferSize != null) {
                connectSocket.setReceiveBufferSize(this.receiveBufferSize);
            }
            if (this.reuseAddress != null) {
                connectSocket.setReuseAddress(this.reuseAddress);
            }
            if (this.sendBufferSize != null) {
                connectSocket.setSendBufferSize(this.sendBufferSize);
            }
            if (this.soLinger != null) {
                connectSocket.setSoLinger(true, this.soLinger);
            }
            if (this.tcpNoDelay != null) {
                connectSocket.setTcpNoDelay(this.tcpNoDelay);
            }
            if (this.trafficClass != null) {
                connectSocket.setTrafficClass(this.trafficClass);
            }
            if (this.oobInline != null) {
                connectSocket.setOOBInline(this.oobInline);
            }
            if (this.soTimeout != null) {
                connectSocket.setSoTimeout(this.soTimeout);
            }
        }
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setSoLinger(on, linger);
        } else {
            if (on) {
                this.soLinger = linger;
            } else {
                this.soLinger = null;
            }
        }
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setSoTimeout(timeout);
        } else {
            this.soTimeout = timeout;
        }
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setTcpNoDelay(on);
        } else {
            this.tcpNoDelay = on;
        }
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        if (this.proxySocket != null) {
            this.proxySocket.setTrafficClass(tc);
        } else {
            this.trafficClass = tc;
        }
    }

    @Override
    public void shutdownInput() throws IOException {
        if (this.proxySocket != null) {
            this.proxySocket.shutdownInput();
        }
        throw new SocketException("Socket is not connected");
    }

    @Override
    public void shutdownOutput() throws IOException {
        if (this.proxySocket != null) {
            this.proxySocket.shutdownOutput();
        }
        throw new SocketException("Socket is not connected");
    }

}
