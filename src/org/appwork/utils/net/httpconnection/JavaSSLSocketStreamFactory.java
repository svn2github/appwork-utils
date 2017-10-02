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
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

/**
 * @author daniel
 *
 */
public class JavaSSLSocketStreamFactory implements SSLSocketStreamFactory {
    private static final JavaSSLSocketStreamFactory INSTANCE = new JavaSSLSocketStreamFactory();

    public static final JavaSSLSocketStreamFactory getInstance() {
        return INSTANCE;
    }

    public static SSLSocketFactory getSSLSocketFactory(final boolean useSSLTrustAll) throws IOException {
        return getSSLSocketFactory(useSSLTrustAll, null);
    }

    public static SSLSocketFactory getSSLSocketFactory(final boolean useSSLTrustAll, final String[] cipherBlacklist) throws IOException {
        final SSLSocketFactory factory;
        if (useSSLTrustAll) {
            factory = TrustALLSSLFactory.getSSLFactoryTrustALL();
        } else {
            factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        return new SSLSocketFactory() {
            /**
             * remove SSL because of POODLE Vulnerability
             *
             * https://www.us-cert.gov/ncas/alerts/TA14-290A
             *
             * @param socket
             */
            private Socket removeSSLProtocol(final Socket socket) {
                if (socket != null && socket instanceof SSLSocket) {
                    final SSLSocket sslSocket = (SSLSocket) socket;
                    final long javaVersion = Application.getJavaVersion();
                    if (javaVersion >= Application.JAVA18) {
                        sslSocket.setEnabledProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" });
                    } else if (javaVersion >= Application.JAVA17) {
                        sslSocket.setEnabledProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" });
                    } else {
                        sslSocket.setEnabledProtocols(new String[] { "TLSv1" });
                    }
                }
                return socket;
            }

            private Socket removeGMCCipherSuit(final Socket socket) {
                if (socket != null && socket instanceof SSLSocket) {
                    // final long javaVersion = Application.getJavaVersion();
                    // https://stackoverflow.com/questions/25992131/slow-aes-gcm-encryption-and-decryption-with-java-8u20/27028067#27028067
                    final boolean gcmWorkaround = true;
                    if (gcmWorkaround || cipherBlacklist != null) {
                        final SSLSocket sslSocket = (SSLSocket) socket;
                        final ArrayList<String> cipherSuits = new ArrayList<String>(Arrays.asList(sslSocket.getEnabledCipherSuites()));
                        final Iterator<String> it = cipherSuits.iterator();
                        boolean updateCipherSuites = false;
                        cipher: while (it.hasNext()) {
                            final String next = it.next();
                            if (gcmWorkaround && StringUtils.containsIgnoreCase(next, "GCM")) {
                                it.remove();
                                updateCipherSuites = true;
                                continue cipher;
                            } else if (cipherBlacklist != null) {
                                for (final String cipherBlacklistEntry : cipherBlacklist) {
                                    if (StringUtils.containsIgnoreCase(next, cipherBlacklistEntry)) {
                                        it.remove();
                                        updateCipherSuites = true;
                                        continue cipher;
                                    }
                                }
                            }
                        }
                        if (updateCipherSuites) {
                            sslSocket.setEnabledCipherSuites(cipherSuits.toArray(new String[0]));
                        }
                    }
                }
                return socket;
            }

            @Override
            public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3) throws IOException {
                return removeGMCCipherSuit(this.removeSSLProtocol(factory.createSocket(arg0, arg1, arg2, arg3)));
            }

            @Override
            public String[] getDefaultCipherSuites() {
                return factory.getDefaultCipherSuites();
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return factory.getSupportedCipherSuites();
            }

            @Override
            public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
                return removeGMCCipherSuit(this.removeSSLProtocol(factory.createSocket(arg0, arg1)));
            }

            @Override
            public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
                return removeGMCCipherSuit(this.removeSSLProtocol(factory.createSocket(arg0, arg1)));
            }

            @Override
            public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException {
                return removeGMCCipherSuit(this.removeSSLProtocol(factory.createSocket(arg0, arg1, arg2, arg3)));
            }

            @Override
            public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
                return removeGMCCipherSuit(this.removeSSLProtocol(factory.createSocket(arg0, arg1, arg2, arg3)));
            }
        };
    }

    protected void verifySSLHostname(final SSLSocket sslSocket, final String host, final boolean trustAll) throws IOException {
        if (!trustAll) {
            final SSLSession sslSession = sslSocket.getSession();
            if (sslSession != null && sslSession.getPeerCertificates().length > 0) {
                final Certificate certificate = sslSession.getPeerCertificates()[0];
                if (certificate instanceof X509Certificate) {
                    final String hostname = host.toLowerCase(Locale.ENGLISH);
                    final ArrayList<String> subjects = new ArrayList<String>();
                    final X509Certificate x509 = (X509Certificate) certificate;
                    subjects.add(new Regex(x509.getSubjectX500Principal().getName(), "CN=(.*?)(,| |$)").getMatch(0));
                    try {
                        final Collection<List<?>> subjectAlternativeNames = x509.getSubjectAlternativeNames();
                        if (subjectAlternativeNames != null) {
                            for (final List<?> subjectAlternativeName : subjectAlternativeNames) {
                                final Integer generalNameType = (Integer) subjectAlternativeName.get(0);
                                switch (generalNameType) {
                                case 1:// rfc822Name
                                case 2:// dNSName
                                    subjects.add(subjectAlternativeName.get(1).toString());
                                    break;
                                }
                            }
                        }
                    } catch (CertificateParsingException e) {
                        e.printStackTrace();
                    }
                    for (String subject : subjects) {
                        if (subject != null) {
                            subject = subject.toLowerCase(Locale.ENGLISH);
                            if (StringUtils.equals(subject, hostname)) {
                                return;
                            } else if (subject.startsWith("*.") && hostname.length() > subject.length() - 1 && hostname.endsWith(subject.substring(1)) && hostname.substring(0, hostname.length() - subject.length() + 1).indexOf('.') < 0) {
                                /**
                                 * http://en.wikipedia.org/wiki/ Wildcard_certificate
                                 */
                                return;
                            }
                        }
                    }
                    throw new SSLHandshakeException("HTTPS hostname wrong:  hostname is <" + hostname + ">");
                }
            }
        }
    }

    @Override
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, final String host, final int port, final boolean autoClose, final boolean trustAll, final String[] cipherBlacklist) throws IOException {
        final SSLSocket sslSocket = (SSLSocket) getSSLSocketFactory(trustAll, cipherBlacklist).createSocket(socketStream.getSocket(), host, port, autoClose);
        // sslSocket.startHandshake();
        // verifySSLHostname(sslSocket, host, trustAll);
        return new SSLSocketStreamInterface() {
            @Override
            public Socket getSocket() {
                return sslSocket;
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return sslSocket.getOutputStream();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return sslSocket.getInputStream();
            }

            @Override
            public void close() throws IOException {
                sslSocket.close();
            }

            @Override
            public String getCipherSuite() {
                return sslSocket.getSession().getCipherSuite();
            }
        };
    }
}
