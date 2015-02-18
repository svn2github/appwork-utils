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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.net.httpconnection.ProxyConnectException;

/**
 * @author daniel
 *
 */
public class HTTPProxySocketConnection extends SocketConnection {

    public HTTPProxySocketConnection(HTTPProxy proxy) {
        super(proxy);
        if (proxy == null || !HTTPProxy.TYPE.HTTP.equals(proxy.getType())) { throw new IllegalArgumentException("proxy must be of type http"); }
    }

    @Override
    protected Socket connectProxySocket(final Socket proxySocket, final SocketAddress endpoint, final StringBuffer logger) throws IOException {
        final InetSocketAddress endPointAddress = (InetSocketAddress) endpoint;
        final OutputStream os = proxySocket.getOutputStream();
        final StringBuilder connectRequest = new StringBuilder();
        connectRequest.append("CONNECT ");
        connectRequest.append(SocketConnection.getHostName(endPointAddress) + ":" + endPointAddress.getPort());
        connectRequest.append(" HTTP/1.1\r\n");
        final String username = this.getProxy().getUser() == null ? "" : this.getProxy().getUser();
        final String password = this.getProxy().getPass() == null ? "" : this.getProxy().getPass();
        if (username.length() > 0 || password.length() > 0) {
            final String basicAuth = "Basic " + new String(Base64.encodeToByte((username + ":" + password).getBytes(), false));
            connectRequest.append("Proxy-Authorization: " + basicAuth + "\r\n");
        }
        connectRequest.append("\r\n");
        /* send CONNECT to proxy */
        os.write(connectRequest.toString().getBytes("ISO-8859-1"));
        os.flush();
        /* parse CONNECT response */
        final InputStream is = proxySocket.getInputStream();
        ByteBuffer headerByteBuffer = HTTPConnectionUtils.readheader(is, true);
        final byte[] headerBytes = new byte[headerByteBuffer.limit()];
        headerByteBuffer.get(headerBytes);
        final String proxyResponseStatus = new String(headerBytes, "ISO-8859-1").trim();
        final int responseCode;
        final String responseCodeString = new Regex(proxyResponseStatus, "HTTP.*? (\\d+)").getMatch(0);
        if (responseCodeString != null) {
            responseCode = Integer.parseInt(responseCodeString);
        } else {
            responseCode = -1;
        }
        switch (responseCode) {
        case 200:
            break;
        case 403:
            throw new ConnectException("403 Connection refused");
        case 407:
            throw new ProxyAuthException(this.getProxy());
        case 504:
            throw new SocketTimeoutException("504 Gateway timeout");
        default:
            throw new ProxyConnectException("Invalid responseCode " + responseCode, this.getProxy());
        }
        /* read rest of CONNECT headers */
        /*
         * Again, the response follows the HTTP/1.0 protocol, so the response
         * line starts with the protocol version specifier, and the response
         * line is followed by zero or more response headers, followed by an
         * empty line. The line separator is CR LF pair, or a single LF.
         */
        while (true) {
            /*
             * read line by line until we reach the single empty line as
             * separator
             */
            if (HTTPConnectionUtils.readheader(is, true).limit() <= 2) {
                /* empty line, <=2, as it may contains \r and/or \n */
                break;
            }
        }
        return proxySocket;
    }
}
