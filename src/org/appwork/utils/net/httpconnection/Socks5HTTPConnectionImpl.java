package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import org.appwork.utils.net.socketconnection.Socks5SocketConnection;

public class Socks5HTTPConnectionImpl extends SocksHTTPconnection {

    public Socks5HTTPConnectionImpl(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
        if (this.proxy == null || !HTTPProxy.TYPE.SOCKS5.equals(this.proxy.getType())) { throw new IllegalArgumentException("proxy must be of type socks5"); }
    }

    @Override
    protected Socket establishConnection() throws IOException {
        final Socks5SocketConnection socket = new Socks5SocketConnection(this.getProxy(), DESTTYPE.DOMAIN);
        socket.connect(this.proxyInetSocketAddress = new InetSocketAddress(this.httpHost, this.httpPort), this.getConnectTimeout(), this.proxyRequest);
        return socket;
    }
}
