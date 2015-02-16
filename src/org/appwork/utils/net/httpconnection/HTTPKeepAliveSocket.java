/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpconnection
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection;

import java.net.InetAddress;
import java.net.Socket;

import org.appwork.utils.StringUtils;

/**
 * @author daniel
 *
 */
public class HTTPKeepAliveSocket {

    private final Socket      socket;
    private final InetAddress localIP;
    private final String      host;

    public InetAddress getLocalIP() {
        return this.localIP;
    }

    public InetAddress[] getRemoteIPs() {
        return this.remoteIPs;
    }

    private final InetAddress[] remoteIPs;

    public Socket getSocket() {
        return this.socket;
    }

    public boolean sameLocalIP(final InetAddress otherIP) {
        return otherIP == null && this.getLocalIP() == null || otherIP != null && this.getLocalIP() != null && this.getLocalIP().equals(otherIP);
    }

    public boolean sameHost(final String otherHost) {
        return StringUtils.equalsIgnoreCase(this.getHost(), otherHost);
    }

    public boolean sameRemoteIPs(final InetAddress remoteIPs[]) {
        if (remoteIPs != null && remoteIPs.length > 0) {
            final InetAddress socketRemoteIP = this.getSocket().getInetAddress();
            for (final InetAddress remoteIP : remoteIPs) {
                if (socketRemoteIP.equals(remoteIP)) {
                    //
                    return true;
                }
            }
            if (this.getRemoteIPs() != null) {
                for (final InetAddress knownRemoteIP : this.getRemoteIPs()) {
                    for (final InetAddress remoteIP : remoteIPs) {
                        if (knownRemoteIP.equals(remoteIP)) {
                            //
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public long getKeepAliveTimeout() {
        return this.keepAliveTimeout;
    }

    public long getRequestsLeft() {
        return Math.max(0, this.getRequestsMax() - this.requests);
    }

    public long getRequestsMax() {
        return this.maxRequests;
    }

    public void increaseRequests() {
        this.requests += 1;
    }

    private final long    keepAliveTimeout;
    private final long    maxRequests;
    private volatile long keepAliveTimestamp = -1;
    private volatile long requests           = 0;
    private final boolean ssl;

    public boolean isSsl() {
        return this.ssl;
    }

    public long getKeepAliveTimestamp() {
        return this.keepAliveTimestamp;
    }

    public void keepAlive() {
        this.keepAliveTimestamp = System.currentTimeMillis() + this.getKeepAliveTimeout();
    }

    public HTTPKeepAliveSocket(final String host, final boolean ssl, final Socket socket, final long keepAliveTimeout, final long maxRequests, final InetAddress localIP, final InetAddress[] remoteIPs) {
        this.host = host;
        this.socket = socket;
        this.localIP = localIP;
        this.remoteIPs = remoteIPs;
        this.keepAliveTimeout = Math.max(0, keepAliveTimeout);
        this.maxRequests = Math.max(0, maxRequests);
        this.ssl = ssl;
    }

    public String getHost() {
        return this.host;
    }
}
