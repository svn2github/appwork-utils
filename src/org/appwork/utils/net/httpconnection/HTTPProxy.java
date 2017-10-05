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
package org.appwork.utils.net.httpconnection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.processes.ProcessBuilderFactory;

public class HTTPProxy {
    public static enum TYPE {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP,
        HTTPS
    }

    public static final HTTPProxy NONE = new HTTPProxy(TYPE.NONE) {
                                           @Override
                                           public void setConnectMethodPrefered(final boolean value) {
                                           }

                                           @Override
                                           public void setLocal(final String local) {
                                           }

                                           @Override
                                           public void setPass(final String pass) {
                                           }

                                           @Override
                                           public void setPort(final int port) {
                                           }

                                           @Override
                                           public void setType(final TYPE type) {
                                               super.setType(TYPE.NONE);
                                           }

                                           @Override
                                           public void setUser(final String user) {
                                           }
                                       };

    public static List<HTTPProxy> getFromSystemProperties() {
        final java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        try {
            {
                /* try to parse http proxy from system properties */
                final String host = System.getProperties().getProperty("http.proxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 80;
                    final String ports = System.getProperty("http.proxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
                    final String user = System.getProperty("http.proxyUser");
                    final String pass = System.getProperty("http.proxyPassword");
                    if (!StringUtils.isEmpty(user)) {
                        pr.setUser(user);
                    }
                    if (!StringUtils.isEmpty(pass)) {
                        pr.setPass(pass);
                    }
                    ret.add(pr);
                }
            }
            {
                /* try to parse http proxy from system properties */
                final String host = System.getProperties().getProperty("https.proxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 443;
                    final String ports = System.getProperty("https.proxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTPS, host, port);
                    final String user = System.getProperty("https.proxyUser");
                    final String pass = System.getProperty("https.proxyPassword");
                    if (!StringUtils.isEmpty(user)) {
                        pr.setUser(user);
                    }
                    if (!StringUtils.isEmpty(pass)) {
                        pr.setPass(pass);
                    }
                    ret.add(pr);
                }
            }
            {
                /* try to parse socks5 proxy from system properties */
                final String host = System.getProperties().getProperty("socksProxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 1080;
                    final String ports = System.getProperty("socksProxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
                    ret.add(pr);
                }
            }
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return ret;
    }

    public static HTTPProxy getHTTPProxy(final HTTPProxyStorable storable) {
        if (storable == null || storable.getType() == null) {
            return null;
        }
        HTTPProxy ret = null;
        switch (storable.getType()) {
        case NONE:
            return HTTPProxy.NONE;
        case DIRECT:
            ret = new HTTPProxy(TYPE.DIRECT);
            ret.setLocal(storable.getAddress());
            break;
        case HTTP:
            ret = new HTTPProxy(TYPE.HTTP);
            ret.setHost(storable.getAddress());
            break;
        case HTTPS:
            ret = new HTTPProxy(TYPE.HTTPS);
            ret.setHost(storable.getAddress());
            break;
        case SOCKS4:
            ret = new HTTPProxy(TYPE.SOCKS4);
            ret.setHost(storable.getAddress());
            break;
        case SOCKS5:
            ret = new HTTPProxy(TYPE.SOCKS5);
            ret.setHost(storable.getAddress());
            break;
        }
        ret.setPreferNativeImplementation(storable.isPreferNativeImplementation());
        ret.setConnectMethodPrefered(storable.isConnectMethodPrefered());
        ret.setResolveHostname(storable.isResolveHostName());
        ret.setPass(storable.getPassword());
        ret.setUser(storable.getUsername());
        ret.setPort(storable.getPort());
        return ret;
    }

    private static String[] getInfo(final String host, final String port) {
        final String[] info = new String[2];
        if (host == null) {
            return info;
        }
        final String tmphost = host.replaceFirst("^https?://", "");
        String tmpport = new org.appwork.utils.Regex(host, ":(\\d+)(/|$)").getMatch(0);
        if (tmpport != null) {
            info[1] = tmpport;
        } else {
            if (port != null) {
                tmpport = new Regex(port, "(\\d+)").getMatch(0);
            }
            if (tmpport != null) {
                info[1] = tmpport;
            } else {
                info[1] = "8080";
            }
        }
        info[0] = new Regex(tmphost, "(.*?)(:\\d+$|:\\d+/|/|$)").getMatch(0);
        return info;
    }

    public static HTTPProxyStorable getStorable(final HTTPProxy proxy) {
        if (proxy == null || proxy.getType() == null) {
            return null;
        }
        final HTTPProxyStorable ret = new HTTPProxyStorable();
        switch (proxy.getType()) {
        case NONE:
            ret.setType(HTTPProxyStorable.TYPE.NONE);
            ret.setAddress(null);
            break;
        case DIRECT:
            ret.setType(HTTPProxyStorable.TYPE.DIRECT);
            ret.setAddress(proxy.getLocal());
            break;
        case HTTP:
            ret.setType(HTTPProxyStorable.TYPE.HTTP);
            ret.setAddress(proxy.getHost());
            break;
        case HTTPS:
            ret.setType(HTTPProxyStorable.TYPE.HTTPS);
            ret.setAddress(proxy.getHost());
            break;
        case SOCKS4:
            ret.setType(HTTPProxyStorable.TYPE.SOCKS4);
            ret.setAddress(proxy.getHost());
            break;
        case SOCKS5:
            ret.setType(HTTPProxyStorable.TYPE.SOCKS5);
            ret.setAddress(proxy.getHost());
            break;
        }
        ret.setConnectMethodPrefered(proxy.isConnectMethodPrefered());
        ret.setPreferNativeImplementation(proxy.isPreferNativeImplementation());
        ret.setResolveHostName(proxy.isResolveHostname());
        ret.setPort(proxy.getPort());
        ret.setPassword(proxy.getPass());
        ret.setUsername(proxy.getUser());
        return ret;
    }

    /**
     * Checks windows registry for proxy settings
     */
    public static List<HTTPProxy> getWindowsRegistryProxies() {
        final java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        try {
            final ProcessBuilder pb = ProcessBuilderFactory.create(new String[] { "reg", "query", "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings" });
            final Process process = pb.start();
            final String result = IO.readInputStreamToString(process.getInputStream());
            process.destroy();
            try {
                final String autoProxy = new Regex(result, "AutoConfigURL\\s+REG_SZ\\s+([^\r\n]+)").getMatch(0);
                if (!StringUtils.isEmpty(autoProxy)) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("AutoProxy.pac Script found: " + autoProxy);
                }
            } catch (final Exception e) {
            }
            final String enabledString = new Regex(result, "ProxyEnable\\s+REG_DWORD\\s+(\\d+x\\d+)").getMatch(0);
            if ("0x0".equals(enabledString)) {
                // proxy disabled
                return ret;
            }
            final String val = new Regex(result, " ProxyServer\\s+REG_SZ\\s+([^\r\n]+)").getMatch(0);
            if (val != null) {
                for (final String vals : val.split(";")) {
                    final String lowerCaseVals = vals.toLowerCase(Locale.ENGLISH);
                    if (lowerCaseVals.startsWith("ftp=")) {
                        continue;
                    }
                    /* parse ip */
                    String proxyurl = new Regex(vals, "(\\d+\\.\\d+\\.\\d+\\.\\d+)").getMatch(0);
                    if (proxyurl == null) {
                        /* parse domain name */
                        proxyurl = new Regex(vals, ".+=(.*?)(:\\d+$|:\\d+/|/|$)").getMatch(0);
                        if (proxyurl == null) {
                            /* parse domain name */
                            proxyurl = new Regex(vals, "=?(.*?)(:\\d+$|:\\d+/|/|$)").getMatch(0);
                        }
                    }
                    final String port = new Regex(vals, ":(\\d+)(/|$)").getMatch(0);
                    if (proxyurl != null) {
                        if (lowerCaseVals.startsWith("socks")) {
                            final int rPOrt = port != null ? Integer.parseInt(port) : 1080;
                            final HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.SOCKS5);
                            pd.setHost(proxyurl);
                            pd.setPort(rPOrt);
                            ret.add(pd);
                        } else if (lowerCaseVals.startsWith("https")) {
                            final int rPOrt = port != null ? Integer.parseInt(port) : 443;
                            final HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.HTTPS);
                            pd.setHost(proxyurl);
                            pd.setPort(rPOrt);
                            ret.add(pd);
                        } else if (lowerCaseVals.startsWith("http")) {
                            final int rPOrt = port != null ? Integer.parseInt(port) : 8080;
                            final HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.HTTP);
                            pd.setHost(proxyurl);
                            pd.setPort(rPOrt);
                            ret.add(pd);
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return ret;
    }

    public static HTTPProxy parseHTTPProxy(final String s) {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        final String type = new Regex(s, "(https?|socks(5|4)|direct)://").getMatch(0);
        final String auth = new Regex(s, "://(.+)@").getMatch(0);
        final String host = new Regex(s, "://(.+@)?(.*?)(/|$)").getMatch(1);
        HTTPProxy ret = null;
        if ("https".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.HTTPS);
            ret.setPort(443);
        } else if ("http".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.HTTP);
            ret.setPort(8080);
        } else if ("socks5".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.SOCKS5);
            ret.setPort(1080);
        } else if ("socks4".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.SOCKS4);
            ret.setPort(1080);
        } else if ("direct".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.DIRECT);
            ret.setLocal(host);
        }
        if (ret != null) {
            final String hostname = new Regex(host, "(.*?)(:\\d+$|$)").getMatch(0);
            final String port = new Regex(host, ".*?:(\\d+)$").getMatch(0);
            if (!StringUtils.isEmpty(hostname)) {
                ret.setHost(hostname);
            }
            if (!StringUtils.isEmpty(port)) {
                ret.setPort(Integer.parseInt(port));
            }
            final String username = new Regex(auth, "(.*?)(:|$)").getMatch(0);
            final String password = new Regex(auth, ".*?:(.+)").getMatch(0);
            if (!StringUtils.isEmpty(username)) {
                ret.setUser(username);
            }
            if (!StringUtils.isEmpty(password)) {
                ret.setPass(password);
            }
            if (!StringUtils.isEmpty(ret.getHost())) {
                return ret;
            }
        }
        return null;
    }

    protected String  local                      = null;
    protected String  user                       = null;
    protected String  pass                       = null;
    protected int     port                       = 80;
    protected String  host                       = null;
    protected TYPE    type                       = TYPE.DIRECT;
    protected boolean useConnectMethod           = false;
    protected boolean preferNativeImplementation = false;
    protected boolean resolveHostname            = false;

    public boolean isResolveHostname() {
        return resolveHostname;
    }

    public void setResolveHostname(boolean resolveHostname) {
        this.resolveHostname = resolveHostname;
    }

    protected HTTPProxy() {
    }

    public HTTPProxy(final HTTPProxy proxy) {
        this.set(proxy);
    }

    public HTTPProxy(final InetAddress direct) {
        this.setType(TYPE.DIRECT);
        this.setLocal(direct.getHostAddress());
    }

    public HTTPProxy(final TYPE type) {
        this.setType(type);
    }

    public HTTPProxy(final TYPE type, final String host, final int port) {
        this.setPort(port);
        this.setType(type);
        this.setHost(HTTPProxy.getInfo(host, Integer.toString(port))[0]);
    }

    public String _toString() {
        if (this.type == TYPE.NONE) {
            return _AWU.T.proxy_none();
        } else if (this.type == TYPE.DIRECT) {
            return _AWU.T.proxy_direct(getLocal());
        } else if (this.type == TYPE.HTTP) {
            String ret = _AWU.T.proxy_http(this.getHost(), this.getPort());
            if (this.isPreferNativeImplementation()) {
                ret = ret + "(prefer native)";
            }
            return ret;
        } else if (this.type == TYPE.HTTPS) {
            String ret = _AWU.T.proxy_https(this.getHost(), this.getPort());
            if (this.isPreferNativeImplementation()) {
                ret = ret + "(prefer native)";
            }
            return ret;
        } else if (this.type == TYPE.SOCKS5) {
            return _AWU.T.proxy_socks5(this.getHost(), this.getPort());
        } else if (this.type == TYPE.SOCKS4) {
            return _AWU.T.proxy_socks4(this.getHost(), this.getPort());
        } else {
            return "UNKNOWN";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public HTTPProxy clone() {
        final HTTPProxy ret = new HTTPProxy();
        ret.cloneProxy(this);
        return ret;
    }

    protected void cloneProxy(final HTTPProxy proxy) {
        if (proxy == null) {
            return;
        }
        this.set(proxy);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof HTTPProxy)) {
            return false;
        }
        final HTTPProxy p = (HTTPProxy) obj;
        if (this.getType() != p.getType()) {
            return false;
        }
        switch (this.getType()) {
        case DIRECT:
            return StringUtils.equals(this.getLocal(), p.getLocal());
        case NONE:
            return true;
        default:
            return StringUtils.equals(this.getHost(), p.getHost()) && StringUtils.equals(StringUtils.isEmpty(this.getUser()) ? null : this.getUser(), StringUtils.isEmpty(p.getUser()) ? null : p.getUser()) && StringUtils.equals(StringUtils.isEmpty(this.getPass()) ? null : this.getPass(), StringUtils.isEmpty(p.getPass()) ? null : p.getPass()) && this.getPort() == p.getPort();
        }
    }

    public boolean equalsWithSettings(HTTPProxy proxy) {
        if (this.equals(proxy)) {
            switch (this.getType()) {
            case HTTP:
            case HTTPS:
                if (this.isConnectMethodPrefered() != proxy.isConnectMethodPrefered()) {
                    return false;
                }
            default:
                return this.isPreferNativeImplementation() == proxy.isPreferNativeImplementation() && isResolveHostname() == proxy.isResolveHostname();
            }
        }
        return false;
    }

    public String getHost() {
        return this.host;
    }

    public String getLocal() {
        return this.local;
    }

    public String getPass() {
        return this.pass;
    }

    public int getPort() {
        return this.port;
    }

    public TYPE getType() {
        return this.type;
    }

    public String getUser() {
        return this.user;
    }

    @Override
    public int hashCode() {
        return HTTPProxy.class.hashCode();
    }

    public boolean isConnectMethodPrefered() {
        return this.useConnectMethod;
    }

    /**
     * this proxy is DIRECT = using a local bound IP
     *
     * @return
     */
    public boolean isDirect() {
        return this.type == TYPE.DIRECT;
    }

    public boolean isLocal() {
        return this.isDirect() || this.isNone();
    }

    /**
     * this proxy is NONE = uses default gateway
     *
     * @return
     */
    public boolean isNone() {
        return this.type == TYPE.NONE;
    }

    /**
     * @return the preferNativeImplementation
     */
    public boolean isPreferNativeImplementation() {
        return this.preferNativeImplementation;
    }

    /**
     * this proxy is REMOTE = using http,socks proxy
     *
     * @return
     */
    public boolean isRemote() {
        return !this.isDirect() && !this.isNone();
    }

    protected void set(final HTTPProxy proxy) {
        if (proxy != null) {
            this.setUser(proxy.getUser());
            this.setHost(proxy.getHost());
            this.setLocal(proxy.getLocal());
            this.setPass(proxy.getPass());
            this.setPort(proxy.getPort());
            this.setType(proxy.getType());
            this.setResolveHostname(proxy.isResolveHostname());
            this.setConnectMethodPrefered(proxy.isConnectMethodPrefered());
            this.setPreferNativeImplementation(proxy.isPreferNativeImplementation());
        }
    }

    public void setConnectMethodPrefered(final boolean value) {
        this.useConnectMethod = value;
    }

    public void setHost(String host) {
        if (host != null) {
            host = host.trim();
        }
        this.host = host;
    }

    /**
     * @param localIP
     *            the localIP to set
     */
    public void setLocal(final String local) {
        if (local != null) {
            this.local = local.trim();
        } else {
            this.local = local;
        }
    }

    public void setPass(final String pass) {
        this.pass = pass;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * @param preferNativeImplementation
     *            the preferNativeImplementation to set
     */
    public void setPreferNativeImplementation(final boolean preferNativeImplementation) {
        this.preferNativeImplementation = preferNativeImplementation;
    }

    public void setType(final TYPE type) {
        this.type = type;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return this._toString();
    }

    /**
     * @param plist
     * @return
     */
    public static List<? extends HTTPProxy> convert(List<Proxy> plist) {
        ArrayList<HTTPProxy> lst = new ArrayList<HTTPProxy>();
        if (plist != null) {
            for (final Proxy p : plist) {
                try {
                    switch (p.type()) {
                    case DIRECT:
                        lst.add(HTTPProxy.NONE);
                        break;
                    case HTTP:
                        String host = null;
                        int port = -1;
                        if (p.address() instanceof InetSocketAddress) {
                            host = ((InetSocketAddress) p.address()).getHostName();
                            port = ((InetSocketAddress) p.address()).getPort();
                        } else {
                            System.out.println("Cannot handle Proxy address: " + p.address());
                            continue;
                        }
                        if (port == 443) {
                            lst.add(new HTTPProxy(TYPE.HTTPS, host, port));
                        } else {
                            lst.add(new HTTPProxy(TYPE.HTTP, host, port));
                        }
                        break;
                    case SOCKS:
                        host = null;
                        port = -1;
                        if (p.address() instanceof InetSocketAddress) {
                            host = ((InetSocketAddress) p.address()).getHostName();
                            port = ((InetSocketAddress) p.address()).getPort();
                        } else {
                            System.out.println("Cannot handle Proxy address: " + p.address());
                            continue;
                        }
                        lst.add(new HTTPProxy(TYPE.SOCKS5, host, port));
                        break;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return lst;
    }
}
