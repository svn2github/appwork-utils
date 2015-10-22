package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import org.appwork.utils.net.socketconnection.Socks5SocketConnection;

public class Socks5HTTPConnectionImpl extends SocksHTTPconnection {

    public Socks5HTTPConnectionImpl(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
        if (this.proxy == null || !HTTPProxy.TYPE.SOCKS5.equals(this.proxy.getType())) {
            throw new IllegalArgumentException("proxy must be of type socks5");
        }
    }

    @Override
    protected Socket createRawConnectionSocket(final InetAddress bindInetAddress) throws IOException {
        final Socks5SocketConnection socket = new Socks5SocketConnection(this.getProxy(), DESTTYPE.DOMAIN);
        socket.setSoTimeout(readTimeout);
        return socket;
    }

    @Override
    protected SocketStreamInterface connect(SocketStreamInterface socketStream) throws IOException {
        final Socket socket = socketStream.getSocket();
        ((Socks5SocketConnection) socket).connect(this.proxyInetSocketAddress = new InetSocketAddress(this.httpHost, this.httpPort), this.getConnectTimeout(), this.proxyRequest);
        return socketStream;
    }
}
