package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.socketconnection.Socks5Socket;

public class Socks5HTTPConnectionImpl extends SocksHTTPconnection {

    public Socks5HTTPConnectionImpl(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
    }

    @Override
    protected void authenticateProxyPlain() throws IOException {
        try {
            Socks5Socket.authPlain(this.getSocksSocket(), this.proxy.getUser(), this.proxy.getPass(), this.proxyRequest);
        } catch (final IOException e) {
            try {
                this.sockssocket.close();
            } catch (final Throwable e2) {
            }
            if (e instanceof HTTPProxyException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        }
    }

    @Override
    protected Socket establishConnection() throws IOException {
        try {
            return Socks5Socket.establishConnection(this.getSocksSocket(), new InetSocketAddress(this.httpHost, this.httpPort), DESTTYPE.DOMAIN, this.proxyRequest);
        } catch (final IOException e) {
            this.disconnect();
            if (e instanceof HTTPProxyException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        }
    }

    @Override
    protected AUTH sayHello() throws IOException {
        try {
            final AUTH authOffer;
            if (!StringUtils.isEmpty(this.proxy.getUser()) || !StringUtils.isEmpty(this.proxy.getPass())) {
                authOffer = AUTH.PLAIN;
            } else {
                authOffer = AUTH.NONE;
            }
            return Socks5Socket.sayHello(this.getSocksSocket(), authOffer, this.proxyRequest);
        } catch (final IOException e) {
            this.disconnect();
            if (e instanceof HTTPProxyException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        }
    }

    @Override
    protected void validateProxy() throws IOException {
        if (this.proxy == null || !HTTPProxy.TYPE.SOCKS5.equals(this.proxy.getType())) { throw new ProxyConnectException("Socks5HTTPConnection: invalid Socks5 Proxy!", this.proxy); }
    }
}
