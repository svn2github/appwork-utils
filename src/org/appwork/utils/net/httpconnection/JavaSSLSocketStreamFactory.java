/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpconnection
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
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
                    // final ArrayList<String> protocols = new
                    // ArrayList<String>(Arrays.asList(sslSocket.getEnabledProtocols()));
                    // final Iterator<String> it = protocols.iterator();
                    // while (it.hasNext()) {
                    // final String next = it.next();
                    // if (StringUtils.containsIgnoreCase(next, "ssl")) {
                    // it.remove();
                    // }
                    // }
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
                    final long javaVersion = Application.getJavaVersion();
                    final boolean gcmWorkaround = javaVersion < 18600000;
                    if (gcmWorkaround) {
                        final SSLSocket sslSocket = (SSLSocket) socket;
                        final ArrayList<String> cipherSuits = new ArrayList<String>(Arrays.asList(sslSocket.getEnabledCipherSuites()));
                        final Iterator<String> it = cipherSuits.iterator();
                        boolean updateCipherSuites = false;
                        while (it.hasNext()) {
                            final String next = it.next();
                            if (gcmWorkaround && StringUtils.containsIgnoreCase(next, "GCM")) {
                                it.remove();
                                updateCipherSuites = true;
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
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, final String host, final int port, final boolean autoClose, final boolean trustAll) throws IOException {
        final SSLSocket sslSocket = (SSLSocket) getSSLSocketFactory(trustAll).createSocket(socketStream.getSocket(), host, port, autoClose);
        sslSocket.startHandshake();
        verifySSLHostname(sslSocket, host, trustAll);
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
