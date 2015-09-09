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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author daniel
 *
 */
public class URLStream {

    private static final class JarFileCache {
        private final JarFile    jarFile;
        private final AtomicLong openInputStreams = new AtomicLong(0);
        private final String     jarFilePath;

        private JarFileCache(final String jarFilePath) throws IOException {
            this.jarFilePath = jarFilePath;
            this.jarFile = new JarFile(jarFilePath);
        }

        private InputStream getInputStream(final String jarEntryName) throws IOException {
            final JarEntry jarEntry = jarFile.getJarEntry(jarEntryName);
            if (jarEntry != null) {
                System.out.println("URLStream(Workaround)|JarFile:" + jarFilePath + "|Entry:" + jarEntryName);
                final InputStream is = jarFile.getInputStream(jarEntry);
                final FilterInputStream ret = new FilterInputStream(is) {
                    private final AtomicBoolean closed = new AtomicBoolean(false);

                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            synchronized (JARFILECACHE) {
                                if (closed.compareAndSet(false, true)) {
                                    openInputStreams.decrementAndGet();
                                    JarFileCache.this.close();
                                }
                            }
                        }
                    }

                    @Override
                    protected void finalize() throws Throwable {
                        close();
                    }
                };
                openInputStreams.incrementAndGet();
                return ret;
            }
            return null;
        }

        private void close() throws IOException {
            synchronized (JARFILECACHE) {
                if (openInputStreams.get() == 0) {
                    try {
                        jarFile.close();
                    } finally {
                        JARFILECACHE.remove(jarFilePath, this);
                    }
                }
            }
        }
    }

    private static final HashMap<String, JarFileCache> JARFILECACHE = new HashMap<String, JarFileCache>();

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
                if (StringUtils.startsWithCaseInsensitive(path, "file:") && StringUtils.contains(path, "!/")) {
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
                    if (jarFileFile != null && jarFileFile.getPath().contains("!/")) {
                        final String jarFilePath = jarFileFile.getPath();
                        final String jarEntryName = path.substring(lastIndex + 2);
                        synchronized (JARFILECACHE) {
                            JarFileCache jarFileCache = JARFILECACHE.get(jarFilePath);
                            if (jarFileCache == null) {
                                jarFileCache = new JarFileCache(jarFilePath);
                                JARFILECACHE.put(jarFilePath, jarFileCache);
                            }
                            boolean close = true;
                            try {
                                final InputStream is = jarFileCache.getInputStream(jarEntryName);
                                if (is != null) {
                                    close = false;
                                    return is;
                                }
                                throw new FileNotFoundException(path);
                            } finally {
                                if (close) {
                                    jarFileCache.close();
                                }
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
