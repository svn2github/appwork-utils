package org.appwork.utils.net.httpconnection;

public class ProxyAuthException extends HTTPProxyException {

    private static final long serialVersionUID = -6230270480610987372L;

    public ProxyAuthException(final HTTPProxy proxy) {
        super();
        this.proxy = proxy;
    }

    public ProxyAuthException(final Throwable e, final HTTPProxy proxy) {
        super(e);
        this.proxy = proxy;
    }

}
