/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author daniel
 *
 */
public class URLStream {

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        InputStream is = openStream(new URL("jar:file:/home/daniel/Temp!/JDownloader.jar!/version.nfo"));
        System.out.println(IO.readInputStreamToString(is));
    }

    /**
     * workaround for http://bugs.java.com/view_bug.do?bug_id=6390779
     *
     * jar:file:/path/!XY/Test.jar works fine
     *
     * jar:file:/path/XY!/Test.jar fails
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static InputStream openStream(URL url) throws IOException {
        if (url != null) {
            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                final String path = url.getPath();
                if (StringUtils.startsWithCaseInsensitive(path, "file:")) {
                    File jarFileFile = null;
                    int lastIndex = 0;
                    while (true) {
                        final int indexOf = path.indexOf(".jar", lastIndex);
                        if (indexOf > 0) {
                            final int index = indexOf + 4;
                            lastIndex = index;
                            final String jarFileString = path.substring(0, index);
                            final URI testJarURI;
                            try {
                                testJarURI = new URI(jarFileString);
                            } catch (URISyntaxException e) {
                                throw new IOException(e);
                            }
                            final File testJarFile = new File(testJarURI);
                            if (testJarFile.exists() && testJarFile.isFile()) {
                                jarFileFile = testJarFile;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (jarFileFile != null && jarFileFile.getPath().contains("!")) {
                        final String jarEntryName = path.substring(lastIndex + 2);
                        final JarFile jarFile = new JarFile(jarFileFile);
                        boolean closeFlag = true;
                        try {
                            final JarEntry jarEntry = jarFile.getJarEntry(jarEntryName);
                            if (jarEntry != null) {
                                final InputStream is = jarFile.getInputStream(jarEntry);
                                final FilterInputStream ret = new FilterInputStream(is) {
                                    @Override
                                    public void close() throws IOException {
                                        try {
                                            super.close();
                                        } finally {
                                            jarFile.close();
                                        }
                                    }

                                    @Override
                                    protected void finalize() throws Throwable {
                                        jarFile.close();
                                    }
                                };
                                closeFlag = false;
                                return ret;
                            } else {
                                throw new FileNotFoundException(path);
                            }
                        } finally {
                            if (closeFlag) {
                                jarFile.close();
                            }
                        }
                    }
                }
            }
            return url.openStream();
        }
        return null;
    }

}
