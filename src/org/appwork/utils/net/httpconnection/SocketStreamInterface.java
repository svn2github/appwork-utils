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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author daniel
 *
 */
public interface SocketStreamInterface {
    public static enum TCP_VERSION {
        TCP4_ONLY,
        TCP4_TCP6,
        TCP6_TCP4,
        SYSTEM
    }

    public static InetAddress[] resolvHostIP(final String host, TCP_VERSION tcpVersion) throws IOException {
        final InetAddress[] ret = HTTPConnectionUtils.resolvHostIP(host);
        if (ret != null) {
            if (tcpVersion == null) {
                tcpVersion = TCP_VERSION.TCP4_ONLY;
            }
            switch (tcpVersion) {
            case TCP4_ONLY:
                final List<InetAddress> ipv4Only = new ArrayList<InetAddress>();
                for (final InetAddress ip : ret) {
                    if (ip instanceof Inet4Address) {
                        ipv4Only.add(ip);
                    }
                }
                return ipv4Only.toArray(new InetAddress[0]);
            case TCP4_TCP6:
                Arrays.sort(ret, new Comparator<InetAddress>() {
                    private final int compare(boolean x, boolean y) {
                        return (x == y) ? 0 : (x ? 1 : -1);
                    }

                    @Override
                    public int compare(InetAddress o1, InetAddress o2) {
                        final boolean x = o1 instanceof Inet6Address;
                        final boolean y = o2 instanceof Inet6Address;
                        return compare(x, y);
                    }
                });
                return ret;
            case TCP6_TCP4:
                Arrays.sort(ret, new Comparator<InetAddress>() {
                    private final int compare(boolean x, boolean y) {
                        return (x == y) ? 0 : (x ? 1 : -1);
                    }

                    @Override
                    public int compare(InetAddress o1, InetAddress o2) {
                        final boolean x = o1 instanceof Inet4Address;
                        final boolean y = o2 instanceof Inet4Address;
                        return compare(x, y);
                    }
                });
                return ret;
            case SYSTEM:
            default:
                return ret;
            }
        }
        return null;
    }

    public Socket getSocket();

    public InputStream getInputStream() throws IOException;

    public OutputStream getOutputStream() throws IOException;

    public void close() throws IOException;
}
