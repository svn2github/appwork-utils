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
package org.appwork.utils.net.socketconnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.net.httpconnection.ProxyConnectException;
import org.appwork.utils.net.httpconnection.ProxyEndpointConnectException;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.net.httpconnection.SocksHTTPconnection.AUTH;
import org.appwork.utils.net.httpconnection.SocksHTTPconnection.DESTTYPE;

/**
 * @author daniel
 *
 */
public class Socks5SocketConnection extends SocketConnection {
    private final DESTTYPE destType;

    public DESTTYPE getDestType() {
        return this.destType;
    }

    public Socks5SocketConnection(HTTPProxy proxy, DESTTYPE destType) {
        super(proxy);
        if (proxy == null || !HTTPProxy.TYPE.SOCKS5.equals(proxy.getType())) {
            throw new IllegalArgumentException("proxy must be of type socks5");
        }
        this.destType = destType;
    }

    @Override
    protected SocketStreamInterface connectProxySocket(final SocketStreamInterface proxySocket, final SocketAddress endpoint, final StringBuffer logger) throws IOException {
        final AUTH authOffer;
        final HTTPProxy proxy = getProxy();
        final String userName = proxy.getUser();
        final String passWord = proxy.getPass();
        if (!StringUtils.isEmpty(userName) || !StringUtils.isEmpty(passWord)) {
            authOffer = AUTH.PLAIN;
        } else {
            authOffer = AUTH.NONE;
        }
        try {
            final AUTH authRequest = Socks5SocketConnection.sayHello(proxySocket, authOffer, logger);
            switch (authRequest) {
            case PLAIN:
                switch (authOffer) {
                case NONE:
                    throw new InvalidAuthException();
                case PLAIN:
                    Socks5SocketConnection.authPlain(proxySocket, userName, passWord, logger);
                    break;
                }
                break;
            default:
                break;
            }
            return Socks5SocketConnection.establishConnection(proxySocket, endpoint, this.getDestType(), logger);
        } catch (final InvalidAuthException e) {
            throw new ProxyAuthException(e, proxy);
        } catch (final EndpointConnectException e) {
            throw new ProxyEndpointConnectException(e, getProxy(), endpoint);
        } catch (final IOException e) {
            throw new ProxyConnectException(e, this.getProxy());
        }
    }

    protected static SocketStreamInterface establishConnection(final SocketStreamInterface proxySocket, final SocketAddress endpoint, DESTTYPE destType, final StringBuffer logger) throws IOException {
        final InetSocketAddress endPointAddress = (InetSocketAddress) endpoint;
        final OutputStream os = proxySocket.getOutputStream();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        /* socks5 */
        bos.write((byte) 5);
        /* tcp/ip connection */
        bos.write((byte) 1);
        /* reserved */
        bos.write((byte) 0);
        /* send ipv4/domain */
        switch (destType) {
        case IPV4:
            final InetAddress address = endPointAddress.getAddress();
            if (address != null) {
                /* we use ipv4 */
                bos.write((byte) 1);
                if (logger != null) {
                    logger.append("->SEND tcp connect request by ipv4:" + address.getHostAddress() + "\r\n");
                }
                bos.write(address.getAddress());
                break;
            } else {
                if (logger != null) {
                    logger.append("->Cannot connect request by ipv4 (unresolved)\r\n");
                }
            }
        case DOMAIN:
            /* we use domain */
            bos.write((byte) 3);
            final String domainString = SocketConnection.getHostName(endPointAddress);
            if (logger != null) {
                logger.append("->SEND tcp connect request by domain:" + domainString + "\r\n");
            }
            final byte[] domainBytes = domainString.getBytes("ISO-8859-1");
            bos.write((byte) domainBytes.length);
            bos.write(domainBytes);
            break;
        default:
            throw new IllegalArgumentException("Unsupported destType");
        }
        /* send port */
        /* network byte order */
        final int port = endPointAddress.getPort();
        bos.write(port >> 8 & 0xff);
        bos.write(port & 0xff);
        bos.writeTo(os);
        os.flush();
        /* read response, 4 bytes and then read rest of response */
        final InputStream is = proxySocket.getInputStream();
        final byte[] read = SocketConnection.ensureRead(is, 4, null);
        final int[] resp = SocketConnection.byteArrayToIntArray(read);
        if (resp[0] != 5) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        switch (resp[1]) {
        case 0:
            break;
        case 3:
            throw new EndpointConnectException("Network is unreachable");
        case 4:
            throw new EndpointConnectException("Host is unreachable");
        case 5:
            throw new EndpointConnectException("Connection refused");
        case 1:
            throw new IOException("Socks5 general server failure");
        case 2:
            throw new EndpointConnectException("Socks5 connection not allowed by ruleset");
        case 6:
        case 7:
        case 8:
            throw new EndpointConnectException("Socks5 could not establish connection, status=" + resp[1]);
        }
        if (resp[3] == 1) {
            /* ipv4 response */
            final byte[] connectedIP = SocketConnection.ensureRead(is, 4, null);
            /* port */
            final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
            if (logger != null) {
                logger.append("<-BOUND IPv4:" + InetAddress.getByAddress(connectedIP) + ":" + (ByteBuffer.wrap(connectedPort).getShort() & 0xffff) + "\r\n");
            }
        } else if (resp[3] == 3) {
            /* domain name response */
            final byte[] length = SocketConnection.ensureRead(is, 1, null);
            final byte[] connectedDomain = SocketConnection.ensureRead(is, SocketConnection.byteToInt(length[0]), null);
            /* port */
            final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
            if (logger != null) {
                logger.append("<-BOUND Domain:" + new String(connectedDomain) + ":" + (ByteBuffer.wrap(connectedPort).getShort() & 0xffff) + "\r\n");
            }
        } else if (resp[3] == 4) {
            /* ipv6 response */
            final byte[] connectedIP = SocketConnection.ensureRead(is, 16, null);
            /* port */
            final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
            if (logger != null) {
                logger.append("<-BOUND IPv6:" + InetAddress.getByAddress(connectedIP) + ":" + (ByteBuffer.wrap(connectedPort).getShort() & 0xffff) + "\r\n");
            }
        } else {
            throw new IOException("Socks5 unsupported address Type " + resp[3]);
        }
        return proxySocket;
    }

    protected static void authPlain(final SocketStreamInterface proxySocket, String userName, String passWord, final StringBuffer logger) throws IOException {
        final String user = userName == null ? "" : userName;
        final String pass = passWord == null ? "" : passWord;
        if (logger != null) {
            logger.append("->AUTH user:pass\r\n");
        }
        final byte[] userNameBytes = user.getBytes("ISO-8859-1");
        final byte[] passWordBytes = pass.getBytes("ISO-8859-1");
        final OutputStream os = proxySocket.getOutputStream();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        /* must be 1 */
        bos.write((byte) 1);
        /* send username */
        bos.write((byte) userNameBytes.length);
        if (userNameBytes.length > 0) {
            bos.write(userNameBytes);
        }
        /* send password */
        bos.write((byte) passWordBytes.length);
        if (passWordBytes.length > 0) {
            bos.write(passWordBytes);
        }
        bos.writeTo(os);
        os.flush();
        /* read response, 2 bytes */
        final InputStream is = proxySocket.getInputStream();
        final byte[] read = SocketConnection.ensureRead(is, 2, null);
        final int[] resp = SocketConnection.byteArrayToIntArray(read);
        if (resp[0] != 1) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        if (resp[1] != 0) {
            if (logger != null) {
                logger.append("<-AUTH Invalid!\r\n");
            }
            throw new InvalidAuthException("Socks5 auth invalid");
        } else {
            if (logger != null) {
                logger.append("<-AUTH Valid!\r\n");
            }
        }
    }

    private final static boolean SENDONLYSINGLEAUTHMETHOD = true;

    public static AUTH sayHello(final SocketStreamInterface proxySocket, AUTH auth, final StringBuffer logger) throws IOException {
        final OutputStream os = proxySocket.getOutputStream();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (logger != null) {
            logger.append("->SOCKS5 Hello\r\n");
        }
        /* socks5 */
        bos.write((byte) 5);
        /* only none ans password/username auth method */
        final boolean plainAuthPossible = AUTH.PLAIN.equals(auth);
        if (plainAuthPossible) {
            if (SENDONLYSINGLEAUTHMETHOD) {
                bos.write((byte) 1);
                if (logger != null) {
                    logger.append("->SOCKS5 Offer Plain Authentication\r\n");
                }
                /* username/password */
                bos.write((byte) 2);
            } else {
                bos.write((byte) 2);
                if (logger != null) {
                    logger.append("->SOCKS5 Offer None&Plain Authentication\r\n");
                }
                /* none */
                bos.write((byte) 0);
                /* username/password */
                bos.write((byte) 2);
            }
        } else {
            bos.write((byte) 1);
            if (logger != null) {
                logger.append("->SOCKS5 Offer None Authentication\r\n");
            }
            /* none */
            bos.write((byte) 0);
        }
        bos.writeTo(os);
        os.flush();
        /* read response, 2 bytes */
        final InputStream is = proxySocket.getInputStream();
        final byte[] read = SocketConnection.ensureRead(is, 2, null);
        final int[] resp = SocketConnection.byteArrayToIntArray(read);
        if (resp[0] != 5) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        if (resp[1] == 255) {
            if (logger != null) {
                logger.append("<-SOCKS5 Authentication Denied\r\n");
            }
            throw new IOException("Socks5HTTPConnection: no acceptable authentication method found");
        }
        if (resp[1] == 2) {
            if (!plainAuthPossible && logger != null) {
                logger.append("->SOCKS5 Plain auth required but not offered!\r\n");
            }
            return AUTH.PLAIN;
        }
        if (resp[1] == 0) {
            return AUTH.NONE;
        }
        throw new IOException("Unsupported auth:" + resp[1]);
    }
}
