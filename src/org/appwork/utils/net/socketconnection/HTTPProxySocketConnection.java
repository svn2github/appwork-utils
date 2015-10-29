/**
 * 
 * ====================================================================================================================================================
 * "AppWork Utilities" License
 * ====================================================================================================================================================
 * Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * Schwabacher Straße 117
 * 90763 Fürth
 * Germany
 * 
 * === Preamble ===
 * This license establishes the terms under which the AppWork Utilities Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * These terms apply to all files that have the "AppWork Utilities" License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 
 * === 3rd Party Licences ===
 * Some parts of the AppWork Utilities use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 * 
 * === Definition: Commercial Usage ===
 * If anybody or any organization is generating income (directly or indirectly) by using "AppWork Utilities" or if there's as much as a
 * sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * If you want to use AppWork Utilities in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * If there is no commercial usage (see definition above), you may use AppWork Utilities under the terms of the
 * "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 
 * If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
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
